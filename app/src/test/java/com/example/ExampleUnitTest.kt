package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCreditHourSystemCatalogAndYear2Courses() {
    val y2Courses = com.example.data.model.CreditHourCatalog.defaultYear2Courses
    assertEquals(6, y2Courses.size)

    val names = y2Courses.map { it.courseName }
    assertTrue(names.contains("اللوجستيات و سلاسل الإمداد"))
    assertTrue(names.contains("محاسبه التكاليف"))
    assertTrue(names.contains("قانون تجاري"))
    assertTrue(names.contains("مبادئ اقتصاد جزئي"))
    assertTrue(names.contains("مبادئ تسويق"))

    assertEquals(18, com.example.data.model.CreditHourCatalog.MAX_CREDIT_HOURS_PER_SEMESTER)
    val totalHours = y2Courses.sumOf { it.creditHours }
    assertEquals(18, totalHours)
  }
}

