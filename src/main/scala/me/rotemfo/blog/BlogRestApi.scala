package me.rotemfo.blog

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.pattern.ask
import me.rotemfo.api.{AkkaConfiguration, RestApi}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Wrapper API around a [[BlogEntityActor]] actor. This pattern is similar in spirit to Akka Typed.
  */
trait BlogService extends AkkaConfiguration {

  import BlogEntityActor._

  private val blogEntity = actorRefFactory.actorOf(BlogEntityActor.props)

  def getPost(id: PostId): Future[MaybePost[PostContent]] =
    (blogEntity ? GetPost(id)).mapTo[MaybePost[PostContent]]

  def getAllPosts: Future[Iterable[BlogPost]] = (blogEntity ? GetAllPosts).mapTo[Iterable[BlogPost]]

  def addPost(content: PostContent): Future[PostAdded] =
    (blogEntity ? AddPost(content)).mapTo[PostAdded]

  def updatePost(id: PostId, content: PostContent): Future[MaybePost[PostUpdated]] =
    (blogEntity ? UpdatePost(id, content)).mapTo[MaybePost[PostUpdated]]

}

/**
  * HTTP routes for the BlogService API.
  */
trait BlogRestApi extends RestApi with BlogService {
  override def route: Route =
    pathPrefix("api") {
      path("blogs") {
        pathEndOrSingleSlash {
          get {
            // GET /api/blog/:id
            onComplete(getAllPosts) {
              case Success(content) => complete((StatusCodes.OK, content))
              case Failure(error) => complete((StatusCodes.InternalServerError, error))
            }
          }
        }
      } ~
        pathPrefix("blog") {
          (pathEndOrSingleSlash & post) {
            // POST /api/blog/
            entity(as[PostContent]) { content =>
              onSuccess(addPost(content)) { added =>
                complete((StatusCodes.Created, added))
              }
            }
          } ~
            pathPrefix(JavaUUID.map(PostId(_))) { id =>
              pathEndOrSingleSlash {
                get {
                  // GET /api/blog/:id
                  onSuccess(getPost(id)) {
                    case Right(content) => complete((StatusCodes.OK, content))
                    case Left(error) => complete((StatusCodes.NotFound, error))
                  }
                } ~
                  put {
                    // PUT /api/blog/:id
                    entity(as[PostContent]) { content =>
                      onSuccess(updatePost(id, content)) {
                        case Right(updated) => complete((StatusCodes.OK, updated))
                        case Left(error) => complete((StatusCodes.NotFound, error))
                      }
                    }
                  }
              }
            }
        }
    }
}
