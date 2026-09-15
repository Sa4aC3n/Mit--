package com.example

import com.example.util.WorkingHoursUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWorkingHoursUtils_standardShift() {
    val wh = "03:00 م - 09:00 م (السبت - الأربعاء)"

    // Saturday 16:00 (4:00 PM) -> Should be OPEN
    val calOpen = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
      set(Calendar.HOUR_OF_DAY, 16)
      set(Calendar.MINUTE, 0)
    }
    assertTrue("Saturday 4 PM should be open", WorkingHoursUtils.isBusinessOpenNow(wh, calOpen))

    // Saturday 10:00 AM -> Should be CLOSED
    val calClosedMorning = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
    }
    assertFalse("Saturday 10 AM should be closed", WorkingHoursUtils.isBusinessOpenNow(wh, calClosedMorning))

    // Friday 16:00 (Friday is closed) -> Should be CLOSED
    val calClosedFriday = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
      set(Calendar.HOUR_OF_DAY, 16)
      set(Calendar.MINUTE, 0)
    }
    assertFalse("Friday should be closed", WorkingHoursUtils.isBusinessOpenNow(wh, calClosedFriday))
  }

  @Test
  fun testWorkingHoursUtils_overnightShift() {
    val wh = "08:00 ص - 02:00 ص (يومياً)"

    // Sunday 23:30 (11:30 PM) -> OPEN
    val calNight = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 30)
    }
    assertTrue("Sunday 11:30 PM should be open", WorkingHoursUtils.isBusinessOpenNow(wh, calNight))

    // Monday 01:30 AM (crosses midnight) -> OPEN
    val calEarlyMorning = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      set(Calendar.HOUR_OF_DAY, 1)
      set(Calendar.MINUTE, 30)
    }
    assertTrue("Monday 1:30 AM should be open", WorkingHoursUtils.isBusinessOpenNow(wh, calEarlyMorning))

    // Monday 04:00 AM -> CLOSED
    val calClosedEarly = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      set(Calendar.HOUR_OF_DAY, 4)
      set(Calendar.MINUTE, 0)
    }
    assertFalse("Monday 4:00 AM should be closed", WorkingHoursUtils.isBusinessOpenNow(wh, calClosedEarly))
  }

  @Test
  fun testWorkingHoursUtils_24Hours() {
    val wh = "طوال الأسبوع (24 ساعة)"
    val cal = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
      set(Calendar.HOUR_OF_DAY, 4)
      set(Calendar.MINUTE, 15)
    }
    assertTrue("24h should always be open", WorkingHoursUtils.isBusinessOpenNow(wh, cal))
    val status = WorkingHoursUtils.getStatusInfo(wh, cal)
    assertEquals("مفتوح الآن", status.label)
  }

  @Test
  fun testWorkingHoursUtils_fridayException() {
    val wh = "09:00 ص - 10:30 م (السبت - الخميس) / الجمعة: 01:30 م - 10:30 م"

    // Friday 2:00 PM -> OPEN
    val calFriOpen = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
      set(Calendar.HOUR_OF_DAY, 14)
      set(Calendar.MINUTE, 0)
    }
    assertTrue("Friday 2 PM should be open", WorkingHoursUtils.isBusinessOpenNow(wh, calFriOpen))

    // Friday 10:00 AM -> CLOSED
    val calFriClosed = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
    }
    assertFalse("Friday 10 AM should be closed", WorkingHoursUtils.isBusinessOpenNow(wh, calFriClosed))
  }
}
