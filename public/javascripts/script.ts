/// <reference path="./mithril.d.ts" />
(()=> {
    
    "use strict";
    
    class Book {
        title: string;
        description: string;
        author: string;
        category: string;
        guid: string;
        
        constructor(book: any) {
            this.title = book.title;
            this.description = book.description;
            this.author = book.author;
            this.category = book.category;
            this.guid = book.guid;
        }
        
        toTableEmenetsWithIndex(i: number): _mithril.MithrilVirtualElement<{}>[] {
            return [
                m("tr", [
                    m("td", i),
                    m("td", [
                        m("a", {
                            href: this.guid
                        }, this.title)
                    ]),
                    m("td", this.author),
                    m("td", this.category)
                ]),
                m("tr", [
                    m("td", {
                        colspan: 4
                    }, m.trust(this.description))
                ])
            ];
        }
    }

    class Query {
        title: string;
        author: string;
        _any: string;
        count: string;
        
        constructor(title: string, author: string, _any: string, count: string) {
            this.title = title;
            this.author = author;
            this._any = _any;
            this.count = count;
        }
        
        toQueryString(): string {
            return [
                ["title", this.title],
                ["author", this.author],
                ["any", this._any],
                ["count", this.count]
            ].filter(qs => qs[1] !== "")
             .map(qs => qs[0] + "=" + qs[1])
             .join("&");
        }
    }

    class Controller {
        list = m.prop<Book[]>([]);
        
        title = m.prop("")
        author = m.prop("")
        _any = m.prop("")
        count = m.prop("20")
        
        onSubmit = ()=> {
            m.request({
                method: "GET",
                url: "/v1/books?" + (new Query(this.title(), this.author(), this._any(), this.count())).toQueryString()
            }).then((data: Array<any>)=>
                this.list(data.map((book: any)=> new Book(book) ))
            );
            return false;
        }
    }
    
    function makeFormGroup<T>(label: string,
                              type: string,
                              attr: _mithril.MithrilBasicProperty<T>): _mithril.MithrilVirtualElement<{}> {
        return m(".form-group", [
            m("label", label),
            m("input.form-control", {
                type: type,
                oninput: m.withAttr("value", attr),
                value: attr()
            })
        ]);
    }

    function view(ctrl: Controller) {
        return m("div", [
            m(".navbar.navbar-inverse", [
                m(".container-fluid", [
                    m("navbar-header", [
                        m(".navbar-brand", "Simple OPAC")
                    ])
                ])
            ]),
            m(".container", [
                m(".row", [
                    m(".col-md-4", [
                        m(".sidebar", [
                            m("form", [
                                makeFormGroup("Title", "text", ctrl.title),
                                makeFormGroup("Author", "text", ctrl.author),
                                makeFormGroup("Any", "text", ctrl._any),
                                makeFormGroup("Count", "number", ctrl.count),
                                m("button.btn.btn-default", {
                                    type: "submit",
                                    onclick: ctrl.onSubmit
                                }, "Search!")
                            ])
                        ])
                    ]),
                    m(".col-md-8", [
                        m("table.table", [
                            m("thead", [
                                m("tr", ["#", "Title", "Author", "Category"].map(cap=>
                                    m("th", cap)
                                ))
                            ]),
                            m("tbody", (()=> 
                                ctrl.list()
                                    .map((book, i) => book.toTableEmenetsWithIndex(i+1))
                                    .reduce((a, b) => a.concat(b), [])
                            )())
                        ])
                    ])
                ])
            ])
        ]);
    }

    m.mount(document.getElementById('library'), { controller: ()=> new Controller, view: view })
})();
