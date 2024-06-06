# exist-ixml-xar

A package that provides a function for invisible XML parsing.

Please note that this is not the 'official' XQuery 4.0 function, see below.
If you want to use the 'official' function in eXist-db 6.2.x, have a look at [this branch in my fork of eXist-db](https://github.com/nverwer/exist/tree/ixml/exist-core).

## Usage

It is assumed that you have already installed eXist-deb and Maven.

Download or clone or fork the git repository, which has all the source files.
In the directory where you have just downloaded the project, do `maven install`. 
This will create a .xar file in the `target` directory.

Start eXist-db.

Install the .xar file into eXist, using the package manager.

Open eXide, and enter the following in a new XQuery document.

```
import module namespace ixml = "http://rakensi.com/exist-db/xquery/functions/ixml";

let $grammar := ``[
 date = year, -'-', month, -'-', day .
 year = d, d, d, d .
month = '0', d | '1', ['0'|'1'|'2'] .
  day = ['0'|'1'|'2'], d | '3', ['0'|'1'] .
   -d = ['0'-'9'] .
]``
let $ixml-parse := ixml:invisible-xml($grammar, map{})
return $ixml-parse('2024-06-07')
```

Press the 'Eval' button. The output should look like:

```
<date>
    <year>2024</year>
    <month>06</month>
    <day>07</day>
</date>
```

## Notes

In [XQuery 4.0](https://qt4cg.org/specifications/xpath-functions-40/Overview.html#ixml-functions) there will be a function

```
fn:invisible-xml(
  $grammar  as item()?  := (),
  $options  as map(*)?  := {}
)  as fn(xs:string) as item()
```

At some point, this will be implemented in eXist-db.
Until then, you can use the `ixml:invisible-xml` function provided by this package.

When the official `invisible-xml` function becomes available, you should remove the `ixml` module import from the example, and rewrite it as:

```
xquery version "3.1";
let $grammar := ``[
 date = year, -'-', month, -'-', day .
 year = d, d, d, d .
month = '0', d | '1', ['0'|'1'|'2'] .
  day = ['0'|'1'|'2'], d | '3', ['0'|'1'] .
   -d = ['0'-'9'] .
]``
let $ixml-parse := fn:invisible-xml($grammar, map{})
return $ixml-parse('2023-06-07')
```
