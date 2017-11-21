import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.client.RequestBuilding._

object WebService {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("ASN-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    var asnLookUp:Map[String,String] = Map()


    Http().singleRequest(Get("http://bgp.potaroo.net/cidr/autnums.html")).flatMap { res =>
      val lines = res.entity.dataBytes.map(_.utf8String)
      val pattern = ">AS((\\d+))\\s*</a> (.*) ".r

      lines.runForeach { line =>
        pattern.findAllIn(line).matchData foreach {
          m => {
            asnLookUp += (m.group(2).toString -> m.group(3).toString)
          }
        }
      }

    }

    val route: Route = {
      pathPrefix("api" / "v1") {
        path("asn"/Segment) { asnNumber =>
            complete {
              HttpEntity(ContentTypes.`application/json`, """{"description":""""+asnLookUp(asnNumber)+""""}""")
            }
        }
      }
    }

    val bindFuture = Http().bindAndHandle(route, "localhost", 8080)
    println("WebService has been started")
  }
}