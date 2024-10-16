package com.rakensi;

import java.util.ArrayList;
import java.util.List;

import org.greenmercury.smax.SmaxElement;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

/**
 * The NER implementations for specific XQuery engines need to wrap the input node for NER in an element.
 * This is a very simple implementation of Element that can be used for wrapping.
 * This is just enough to traverse to child nodes. No parent element, no attributes, no owner document.
 * @author Rakensi
 *
 */
public class VerySimpleElementImpl implements Element
{

  private String namespaceUri;
  private String namespacePrefix;
  private String localName;
  private String qualifiedName;
  private List<Node> children;
  private VerySimpleAttributesImpl attributes;

  public VerySimpleElementImpl(String name)
  {
    this();
    this.setName(null, name, name);
  }

  public VerySimpleElementImpl()
  {
    this.children = new ArrayList<Node>();
    this.attributes = new VerySimpleAttributesImpl();
  }

  public void setName(String namespaceUri, String localName, String qualifiedName) {
    this.namespaceUri = namespaceUri != null ? namespaceUri : "";
    this.namespacePrefix = qualifiedName.contains(":") ? qualifiedName.substring(0, qualifiedName.indexOf(':')) : "";
    this.localName = localName;
    this.qualifiedName = qualifiedName;
  }

  @Override
  public String getNodeName()
  {
    return qualifiedName;
  }

  @Override
  public String getNodeValue() throws DOMException
  {
    return null;
  }

  @Override
  public void setNodeValue(String nodeValue) throws DOMException
  {
  }

  @Override
  public short getNodeType()
  {
    return Node.ELEMENT_NODE;
  }

  @Override
  public Node getParentNode()
  {
    return null;
  }

  @Override
  public NodeList getChildNodes()
  {
    return new NodeList() {
      @Override
      public Node item(int index) {
        return VerySimpleElementImpl.this.children.get(index);
      }
      @Override
      public int getLength() {
        return VerySimpleElementImpl.this.children.size();
      }};
  }

  @Override
  public Node getFirstChild()
  {
    return this.children.get(0);
  }

  @Override
  public Node getLastChild()
  {
    return this.children.get(this.children.size() - 1);
  }

  @Override
  public Node getPreviousSibling()
  {
    return null;
  }

  @Override
  public Node getNextSibling()
  {
    return null;
  }

  @Override
  public NamedNodeMap getAttributes()
  {
    return attributes;
  }

  @Override
  public Document getOwnerDocument()
  {
    return null;
  }

  @Override
  public Node insertBefore(Node newChild, Node refChild) throws DOMException
  {
    int refIndex = this.children.indexOf(refChild);
    if (refIndex < 0) throw new DOMException(DOMException.NOT_FOUND_ERR, "The reference child is not a child of this element.");
    children.add(refIndex, newChild);
    return newChild;
  }

  @Override
  public Node replaceChild(Node newChild, Node oldChild) throws DOMException
  {
    int refIndex = this.children.indexOf(oldChild);
    if (refIndex < 0) throw new DOMException(DOMException.NOT_FOUND_ERR, "The old child is not a child of this element.");
    return children.set(refIndex, newChild);
  }

  @Override
  public Node removeChild(Node oldChild) throws DOMException
  {
    int refIndex = this.children.indexOf(oldChild);
    if (refIndex < 0) throw new DOMException(DOMException.NOT_FOUND_ERR, "The old child is not a child of this element.");
    return children.remove(refIndex);
  }

  @Override
  public Node appendChild(Node newChild) throws DOMException
  {
    if (newChild instanceof Document || newChild instanceof DocumentFragment) {
      newChild = newChild.getFirstChild();
    }
    children.add(newChild);
    return newChild;
  }

  @Override
  public boolean hasChildNodes()
  {
    return children.size() > 1;
  }

  @Override
  public Node cloneNode(boolean deep)
  {
    if (deep) throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
    VerySimpleElementImpl clone = new VerySimpleElementImpl();
    clone.setName(namespaceUri, localName, qualifiedName);
    children.forEach(child -> clone.appendChild(child.cloneNode(deep)));
    return clone;
  }

  @Override
  public void normalize()
  {
  }

  @Override
  public boolean isSupported(String feature, String version)
  {
    return false;
  }

  @Override
  public String getNamespaceURI()
  {
    return namespaceUri;
  }

  @Override
  public String getPrefix()
  {
    return namespacePrefix;
  }

  @Override
  public void setPrefix(String prefix) throws DOMException
  {
    this.namespacePrefix = prefix;
  }

  @Override
  public String getLocalName()
  {
    return localName;
  }

  @Override
  public boolean hasAttributes()
  {
    return false;
  }

  @Override
  public String getBaseURI()
  {
    return null;
  }

  @Override
  public short compareDocumentPosition(Node other) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public String getTextContent() throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public void setTextContent(String textContent) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public boolean isSameNode(Node other)
  {
    return this == other;
  }

  @Override
  public String lookupPrefix(String namespaceURI)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public boolean isDefaultNamespace(String namespaceURI)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public String lookupNamespaceURI(String prefix)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public boolean isEqualNode(Node arg)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public Object getFeature(String feature, String version)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public Object setUserData(String key, Object data, UserDataHandler handler)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public Object getUserData(String key)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public String getTagName()
  {
    return this.qualifiedName;
  }

  @Override
  public String getAttribute(String name)
  {
    return null;
  }

  @Override
  public void setAttribute(String name, String value) throws DOMException
  {
  }

  @Override
  public void removeAttribute(String name) throws DOMException
  {
  }

  @Override
  public Attr getAttributeNode(String name)
  {
    return null;
  }

  @Override
  public Attr setAttributeNode(Attr newAttr) throws DOMException
  {
    return null;
  }

  @Override
  public Attr removeAttributeNode(Attr oldAttr) throws DOMException
  {
    return null;
  }

  @Override
  public NodeList getElementsByTagName(String name)
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public String getAttributeNS(String namespaceURI, String localName) throws DOMException
  {
    return null;
  }

  @Override
  public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException
  {
  }

  @Override
  public void removeAttributeNS(String namespaceURI, String localName) throws DOMException
  {
  }

  @Override
  public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException
  {
    return null;
  }

  @Override
  public Attr setAttributeNodeNS(Attr newAttr) throws DOMException
  {
    return null;
  }

  @Override
  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public boolean hasAttribute(String name)
  {
    return false;
  }

  @Override
  public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException
  {
    return false;
  }

  @Override
  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  @Override
  public void setIdAttribute(String name, boolean isId) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }

  @Override
  public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException
  {
    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not implemented.");
  }


  public class VerySimpleAttributesImpl implements NamedNodeMap {

    @Override
    public Node getNamedItem(String name)
    {
      return null;
    }

    @Override
    public Node setNamedItem(Node arg) throws DOMException
    {
      return null;
    }

    @Override
    public Node removeNamedItem(String name) throws DOMException
    {
      return null;
    }

    @Override
    public Node item(int index)
    {
      return null;
    }

    @Override
    public int getLength()
    {
      return 0;
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException
    {
      return null;
    }

    @Override
    public Node setNamedItemNS(Node arg) throws DOMException
    {
      return null;
    }

    @Override
    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException
    {
      return null;
    }}

}
