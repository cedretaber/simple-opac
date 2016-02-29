/// <reference path="./mithril.d.ts" />
(function () {
    "use strict";
    var Book = (function () {
        function Book(book) {
            this.title = book.title;
            this.description = book.description;
            this.author = book.author;
            this.category = book.category;
            this.guid = book.guid;
        }
        Book.prototype.toTableEmenetsWithIndex = function (i) {
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
        };
        return Book;
    }());
    var Query = (function () {
        function Query(title, author, _any, count) {
            this.title = title;
            this.author = author;
            this._any = _any;
            this.count = count;
        }
        Query.prototype.toQueryString = function () {
            return [
                ["title", this.title],
                ["author", this.author],
                ["any", this._any],
                ["count", this.count]
            ].filter(function (qs) { return qs[1] !== ""; })
                .map(function (qs) { return qs[0] + "=" + qs[1]; })
                .join("&");
        };
        return Query;
    }());
    var Controller = (function () {
        function Controller() {
            var _this = this;
            this.list = m.prop([]);
            this.title = m.prop("");
            this.author = m.prop("");
            this._any = m.prop("");
            this.count = m.prop("20");
            this.onSubmit = function () {
                m.request({
                    method: "GET",
                    url: "/v1/books?" + (new Query(_this.title(), _this.author(), _this._any(), _this.count())).toQueryString()
                }).then(function (data) {
                    return _this.list(data.map(function (book) { return new Book(book); }));
                });
                return false;
            };
        }
        return Controller;
    }());
    function makeFormGroup(label, type, attr) {
        return m(".form-group", [
            m("label", label),
            m("input.form-control", {
                type: type,
                oninput: m.withAttr("value", attr),
                value: attr()
            })
        ]);
    }
    function view(ctrl) {
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
                                m("tr", ["#", "Title", "Author", "Category"].map(function (cap) {
                                    return m("th", cap);
                                }))
                            ]),
                            m("tbody", (function () {
                                return ctrl.list()
                                    .map(function (book, i) { return book.toTableEmenetsWithIndex(i + 1); })
                                    .reduce(function (a, b) { return a.concat(b); }, []);
                            })())
                        ])
                    ])
                ])
            ])
        ]);
    }
    m.mount(document.getElementById('library'), { controller: function () { return new Controller; }, view: view });
})();
