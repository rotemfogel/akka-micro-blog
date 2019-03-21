package me.rotemfo.blog

import akka.actor.Props
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.{Future, Promise}

/**
  * Aggregate root entity for holding the state of a blog.
  */
class BlogEntityActor extends PersistentActor {

  import BlogEntityActor._
  import context._

  private var state = BlogState()

  // in order to really match the Lagom example properly, we'd create an actor for every blog post, though that
  // sounds a bit overkill to me. then our persistenceId would be something like s"blog-$id".
  override def persistenceId: String = "blog"

  override def receiveCommand: Receive = {
    case GetAllPosts =>
      sender() ! state.all
    case GetPost(id) =>
      sender() ! state(id)
    case AddPost(content) =>
      handleEvent(PostAdded(PostId(), content)) pipeTo sender()
      ()
    case UpdatePost(id, content) =>
      state(id) match {
        case response @ Left(_) => sender() ! response
        case Right(_) =>
          handleEvent(PostUpdated(id, content)) pipeTo sender()
          ()
      }
  }

  private def handleEvent[E <: BlogEvent](e: => E): Future[E] = {
    val p = Promise[E]
    persist(e) { event =>
      p.success(event)
      state += event
      system.eventStream.publish(event)
      if (lastSequenceNr != 0 && lastSequenceNr % 1000 == 0) saveSnapshot(state)
    }
    p.future
  }

  override def receiveRecover: Receive = {
    case event: BlogEvent =>
      state += event
    case SnapshotOffer(_, snapshot: BlogState) =>
      state = snapshot
  }

}

object BlogEntityActor {
  def props = Props(new BlogEntityActor)

  sealed trait BlogCommand

  final case class GetPost(id: PostId) extends BlogCommand

  final case object GetAllPosts extends BlogCommand

  final case class AddPost(content: PostContent) extends BlogCommand

  final case class UpdatePost(id: PostId, content: PostContent) extends BlogCommand

  sealed trait BlogEvent {
    val id: PostId
    val content: PostContent
  }

  final case class PostAdded(id: PostId, content: PostContent) extends BlogEvent

  final case class PostUpdated(id: PostId, content: PostContent) extends BlogEvent

  final case class PostNotFound(id: PostId) extends RuntimeException(s"Blog post not found with id $id")

  type MaybePost[+A] = Either[PostNotFound, A]

  final case class BlogState(posts: Map[PostId, PostContent]) {
    def apply(id: PostId): MaybePost[PostContent] = posts.get(id).toRight(PostNotFound(id))
    def all: Iterable[BlogPost] = posts.map({case (k,v) => BlogPost(k, v)})
    def +(event: BlogEvent): BlogState = BlogState(posts.updated(event.id, event.content))
  }

  object BlogState {
    def apply(): BlogState = BlogState(Map.empty)
  }

}