# Simple OPAC

NDLのAPIを使って簡単な書誌情報を表示します。

## これは何？

Play2 Framework 及び Akka の練習用のアプリです。

## 使い方

DBもKVSも利用しないので、SBTがあれば動かす事ができます。

```
$ sbt run
```

これで`localhost:9000`に立ち上がるので、ブラウザでアクセスしてみましょう。

## 著作権表記

本アプリケーションは国立国会図書館サーチのAPIを利用しております。

[![NDL Search](http://iss.ndl.go.jp/information/wp-content/uploads/2011/12/ndlsearch_banner_011.jpg)](http://iss.ndl.go.jp/)

## License

MIT
