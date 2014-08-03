/**
  * Copyright 2014 Pascal Voitot (@mandubian)
  *
  * But deeply inspired by Scala Async project <https://github.com/scala/async>
  */
import org.scalatest._

import scalaz.{Free, Coyoneda}

import scala.concurrent._
import shapeless.test._
import scala.collection.{ GenTraversable, GenTraversableLike }
import scala.collection.generic.CanBuildFrom

class ShapelessSpec extends FlatSpec with Matchers {
  import shapeless._
  import poly._
  import ShapelessExt._
  import nat._
  import syntax.sized._

  "ShapelessExt" should "try" in {

    implicit class Rich[A](val a: A) {
      def +[B](b: B)(implicit hm: HMonoid[A, B]) = hm.append(a, b)
    }

    // HList HMonoid
    val s = (1 :: "string" :: HNil) + (true :: HNil)
    s should equal (1 :: "string" :: true :: HNil)

    val s1 = (1 :: "string" :: HNil) + HNil
    s1 should equal (1 :: "string" :: HNil)

    // Nat HMonoid
    val s2 = Nat(1) + Nat(10)
    s2 should equal (Nat(11))

    val s21 = Nat(1) + Nat(0)
    s21 should equal (Nat(1))

    val s22 = Nat(0) + Nat(1)
    s22 should equal (Nat(1))

    // List HMonoid
    val s3 = List(1, 2) + List(3, 4, 5)
    s3 should contain theSameElementsAs (List(1, 2, 3, 4, 5))
    implicitly[Nil.type => scala.collection.GenTraversableLike[Int, List[Int]]]
    //val s32 = List(1, 2) + implicitly[HMonoid[List[Int], List[Int]]].zero
    val s32 = List(1, 2) + Nil
    s32 should contain theSameElementsAs (List(1, 2))
    val s33 = Nil + List(1, 2)
    s33 should contain theSameElementsAs (List(1, 2))

    // Map HMonoid
    val s4 = Map("1" -> 1, "2" -> 2) + Map("3" -> 3, "4" -> 4, "5" -> 5)
    s4 should contain theSameElementsAs (Map("1" -> 1, "2" -> 2, "3" -> 3, "4" -> 4, "5" -> 5))
    val s42 = Map("1" -> 1, "2" -> 2) + Map()
    s42 should contain theSameElementsAs (Map("1" -> 1, "2" -> 2))
    val s43 = Map() + Map("1" -> 1, "2" -> 2)
    s43 should contain theSameElementsAs (Map("1" -> 1, "2" -> 2))

    // Sized HMonoid
    val s5 = List(1, 2).sized(2).get + List(3, 4, 5).sized(3).get
    typed[Sized[List[Int], _5]](s5)
    s5.unsized should contain theSameElementsAs (List(1, 2, 3, 4, 5))

    val s51 = List(1, 2).sized(2).get + List.empty[Int].sized(0).get
    s51.unsized should contain theSameElementsAs (List(1, 2))

    val s52 = List.empty[Int].sized(0).get + List(1, 2).sized(2).get
    s52.unsized should contain theSameElementsAs (List(1, 2))

    // Option HMonoid
    val s6 = List(1, 2).sized(2) + List(3, 4, 5).sized(3)
    typed[Sized[List[Int], _5]](s6.get)
    s6.get.unsized should contain theSameElementsAs (List(1, 2, 3, 4, 5))

    val s7 = Some(10) + Some(5)
    s7 should equal (Some(15))

    val s71 = Some(10) + None
    s71 should equal (Some(10))

    val s72 = None + Some(10)
    s72 should equal (Some(10))
  }

  "ShapelessExt" should "manage HFunctor" in {

    object f extends Poly1 {
      implicit def caseInt     = at[Int]    (x => "foo_"+x.toString)
      implicit def caseBoolean = at[Boolean](x => "foo_"+x.toString)
      implicit def caseString  = at[String] (x => "foo_"+x)
    }

    object g extends Poly1 {
      implicit def caseString  = at[String] (x => Option(x))
    }

    def map[HA](ha: HA)(f: Poly)(implicit hf: HFunctor[HA, f.type]) = hf.map(ha)(f)

    map(1 :: "string" :: HNil)(f) should equal ("foo_1" :: "foo_string" :: HNil)
    map(HNil)(f) should equal (HNil)

    map(List(1, 2, 3))(f) should equal (List("foo_1", "foo_2", "foo_3"))
    map(List("alpha", "beta", "gamma"))(f) should equal (List("foo_alpha", "foo_beta", "foo_gamma"))
    map(List.empty[Int])(f) should equal (List())

    map(map(List(1, 2, 3))(f))(g) should equal (map(List(1, 2, 3))(g compose f))

    map(List(1, 2, 3).sized(3).get)(f).unsized should equal (List("foo_1", "foo_2", "foo_3"))
    map(List.empty[Int].sized(0).get)(f).unsized should equal (List())
  }

  "ShapelessExt" should "manage HApply" in {
    object f extends Poly1 {
      implicit def caseInt     = at[Int]    (x => "foo_"+x.toString)
      implicit def caseBoolean = at[Boolean](x => "foo_"+x.toString)
      implicit def caseString  = at[String] (x => "foo_"+x)
    }

    object g extends Poly1 {
      implicit def caseString  = at[String] (x => Option(x))
      implicit def caseInt     = at[Int] (x => Option(x))
    }

    def apply[HA, HF](ha: HA)(f: HF)(implicit hap: HApply[HA, HF]) = hap.ap(ha)(f)

    apply(1 :: "string" :: HNil)(f :: g :: HNil) should equal ("foo_1" :: "foo_string" :: Some(1) :: Some("string") :: HNil)
  }
}