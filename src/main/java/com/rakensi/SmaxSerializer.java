package com.rakensi;

import java.util.Stack;

import org.greenmercury.smax.Balancing;
import org.greenmercury.smax.SmaxDocument;
import org.greenmercury.smax.SmaxElement;

import de.bottlecaps.markup.blitz.Errors;
import de.bottlecaps.markup.blitz.Serializer;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;

public class SmaxSerializer implements Serializer<SmaxDocument>
{

  private SmaxDocument document;
  private Stack<SmaxElement> newElements;
  private int attributeLevel;
  private String attributeName;
  private StringBuilder content;
  private int charPointer;

  public SmaxSerializer(SmaxDocument document) {
    this.document = document;
    newElements = new Stack<>();
    attributeLevel = 0;
    attributeName = null;
    content = new StringBuilder();
    charPointer = 0;
  }

  @Override
  public void startNonterminal(String name) {
    if (attributeLevel == 0) {
      SmaxElement newElement = new SmaxElement(name).setStartPos(charPointer);
      newElements.push(newElement);
    }
  }

  @Override
  public void endNonterminal(String name) {
    if (attributeLevel == 0) {
      SmaxElement newElement = newElements.pop().setEndPos(charPointer);
      document.insertMarkup(newElement, Balancing.INNER);
    }
  }

  @Override
  public void startAttribute(String name) {
    ++attributeLevel;
    attributeName = name;
    content.setLength(0);
  }

  @Override
  public void endAttribute() {
    newElements.peek().setAttribute(attributeName, content.toString());
    content.setLength(0);
    --attributeLevel;
  }

  @Override
  public void terminal(int codepoint) {
    String text = Character.toString(codepoint);
    charPointer += text.length();
    if (! UnicodeCategory.xmlChar.containsCodepoint(codepoint))
      Errors.D04.thro(text);
    if (attributeLevel > 0) {
      content.append(text);
    }
  }

  @Override
  public void excluded(int length)
  {
    charPointer += length;
  }

  @Override
  public SmaxDocument getSerialization()
  {
    return this.document;
  }

}
