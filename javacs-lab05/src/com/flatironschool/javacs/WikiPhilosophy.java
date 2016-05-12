package com.flatironschool.javacs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.*;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.select.Elements;

public class WikiPhilosophy {
	
	final static WikiFetcher wf = new WikiFetcher();
	
	/**
	 * Tests a conjecture about Wikipedia and Philosophy.
	 * 
	 * https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy
	 * 
	 * 1. Clicking on the first non-parenthesized, non-italicized link
     * 2. Ignoring external links, links to the current page, or red links
     * 3. Stopping when reaching "Philosophy", a page with no links or a page
     *    that does not exist, or when a loop occurs
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		ArrayList<String> urls = new ArrayList<>();

		System.out.println("Fetching random Wikipedia page...");
		Connection conn = Jsoup.connect("https://en.wikipedia.org/wiki/Special:Random");
		urls.add(conn.get().location()); // First page to visit

		while (true) {
			// Print urls as it crawls through wikipedia
			System.out.println(urls.get(urls.size()-1));

			// If Philosophy is reached, exit with success code.
			if (urls.get(urls.size() - 1).equals("https://en.wikipedia.org/wiki/Philosophy"))
				break;

			Elements paragraphs = wf.fetchWikipedia(urls.get(urls.size() - 1));

			boolean foundlink = false;
			for (Element p : paragraphs) {

				int inparens = 0; //Used to keep track of nested parenthesis

				Iterable<Node> iter = new WikiNodeIterable(p);
				for (Node node : iter) {
					if (node instanceof TextNode) {
						if (node.toString().contains("("))
							inparens++;
						if (node.toString().contains(")"))
							inparens--;
					}

					if (inparens == 0) { // Skip elements inside parenthesis.
						if (node instanceof Element) {
							if (((Element) node).tag().getName().equals("a")) { //Find links

								// Skip elements in italics
								boolean skip = false;
								for (Element e : ((Element) node).parents()) {
									if (e.tag().getName().equals("i") || e.tag().getName().equals("em"))
										skip = true;
								} if (skip) continue;

								//Skip uppercase links
								if (Character.isUpperCase(((Element) node).text().charAt(0)))
									continue;

								//Skip red links
								if (((Element) node).className().equals("new")) continue;

								String href = ((Element) node).attr("href");

								//Skip external links and ID jumpers
								if (href.charAt(0) != '/') continue;

								//Skip links to current page
								if (href.equals(urls.get(urls.size() - 1).substring(24)))
									continue;

								String url = "https://en.wikipedia.org" + href;
								if (!urls.contains(url)) {
									urls.add(url);
									foundlink = true;
									break;
								} else {
									System.err.println("Returned to an already seen page: " + href);
									System.exit(1);
								}

							}
						}
					}
				}
				if (foundlink) break;
			}

			if (!foundlink) {
				System.err.println("Couldn't find valid links");
				System.exit(1);
			}
		}
	}
}
