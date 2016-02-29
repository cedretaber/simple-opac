package forms

case class SearchForm(title: Option[String],
                      author: Option[String],
                      any: Option[String],
                      count: Option[Int])
