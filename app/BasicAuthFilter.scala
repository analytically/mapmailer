import play.Logger
import play.api.Play
import sun.misc.BASE64Decoder
import play.api.mvc._
import scala.concurrent.Future

object BasicAuthFilter extends Filter {
  private lazy val username = Play.current.configuration.getString("auth.username")
  private lazy val password = Play.current.configuration.getString("auth.password")

  private val unauthResult = Results.Unauthorized.withHeaders(("WWW-Authenticate", "Basic realm=\"mapmailer\""))
  private val basicSt = "basic "

  private def getUserIPAddress(request: RequestHeader): String = {
    request.headers.get("x-forwarded-for").getOrElse(request.remoteAddress.toString)
  }

  private def logFailedAttempt(requestHeader: RequestHeader) = {
    Logger.warn(s"IP address ${getUserIPAddress(requestHeader)} failed to log in, requested uri: ${requestHeader.uri}")
  }

  private def decodeBasicAuth(auth: String): Option[(String, String)] = {
    if (auth.length() < basicSt.length()) {
      return None
    }

    val basicReqSt = auth.substring(0, basicSt.length())
    if (basicReqSt.toLowerCase != basicSt) {
      return None
    }
    val basicAuthSt = auth.replaceFirst(basicReqSt, "")

    // not thread safe
    val decoder = new BASE64Decoder()
    val decodedAuthSt = new String(decoder.decodeBuffer(basicAuthSt), "UTF-8")

    val usernamePassword = decodedAuthSt.split(":")
    if (usernamePassword.length >= 2) {
      // account for ":" in passwords
      return Some(usernamePassword(0), usernamePassword.splitAt(1)._2.mkString)
    }
    None
  }

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (username.isEmpty || password.isEmpty) {
      return nextFilter(requestHeader)
    }

    requestHeader.headers.get("authorization").map { basicAuth =>
      decodeBasicAuth(basicAuth) match {
        case Some((user, pass)) =>
          if (username.get == user && password.get == pass) {
            return nextFilter(requestHeader)
          }

        case _ =>
      }

      logFailedAttempt(requestHeader)
      return Future.successful(unauthResult)
    }.getOrElse({
      logFailedAttempt(requestHeader)
      Future.successful(unauthResult)
    })
  }
}
