package tech.jknair.calendarsample

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*


class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions(123, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }

    private fun calendarTest() {
        val calendarList = getCalendarList(this)
        val id = calendarList.firstOrNull()?.id
        Log.w("calendarTest", "primary calendar id ${id}")
        insertSampleEvent(this, id ?: return)
        val eventList = getEventsList(this, id)
    }

    private fun checkPermissions(callbackId: Int, vararg permissionsIds: String) {
        var permissions = true
        for (permissionsId in permissionsIds) {
            permissions = permissions
                    && ContextCompat.checkSelfPermission(
                this,
                permissionsId
            ) == PackageManager.PERMISSION_GRANTED;
        }

        if (!permissions) {
            ActivityCompat.requestPermissions(this, permissionsIds, callbackId)
        } else {
            calendarTest()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            calendarTest()
        } else {
            finish()
        }
    }

}

data class CalendarRow(
    val id: Long,
    val displayName: String,
    val accountName: String
)

data class EventRow(
    val id: Long,
    val title: String,
    val startTimeInMillis: Long
)

fun getCalendarList(cxt: Context): Set<CalendarRow> {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.IS_PRIMARY} = 1"
    val contentResolver = cxt.contentResolver
    var cursor = contentResolver.query(
        uri, arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        ), selection, null, null
    )

    if(cursor == null || cursor.count == 0) {
        cursor?.close()
        cursor = contentResolver.query(
            uri, arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
            ), "${CalendarContract.Calendars.VISIBLE} = 1", null, null
        )
    }

    val calendarRowSet = mutableSetOf<CalendarRow>()
    while (cursor?.moveToNext() == true) {
        val id = cursor.getLong(0)
        val displayName = cursor.getString(1)
        val accountName = cursor.getString(2)
        calendarRowSet.add(CalendarRow(id, displayName, accountName))
    }
    cursor?.close()
    Log.w(
        "getCalendarList",
        "cursor exhausted list : ${if (calendarRowSet.isEmpty()) "isEmpty" else ""} ${calendarRowSet.joinToString(
            "\n"
        )}"
    )
    return calendarRowSet
}

fun getEventsList(cxt: Context, calendarId: Long) {
    val uri = CalendarContract.Events.CONTENT_URI
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.CALENDAR_ID} == ?"
    val selectionArgs = arrayOf("${Calendar.getInstance().timeInMillis}", "$calendarId")
    val contentResolver = cxt.contentResolver
    val cursor = contentResolver.query(
        uri, arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        ), selection, selectionArgs, null
    )

    val eventRowSet = mutableSetOf<EventRow>()
    while (cursor?.moveToNext() == true) {
        val id = cursor.getLong(0)
        val title = cursor.getString(1)
        val startTime = cursor.getLong(2)
        eventRowSet.add(EventRow(id, title, startTime))
    }
    cursor?.close()

    Log.w("getEventsList", "getEventsList: events ${eventRowSet}")


}

// sessionId - sessionid 222 - eventid 212

fun insertSampleEvent(cxt: Context, calendarId: Long) {

    val insertUri = cxt.contentResolver.insert(CalendarContract.Events.CONTENT_URI, ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, "Session Name")
        put(CalendarContract.Events.DESCRIPTION, "sessionId:_lowercase_")
        put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Kolkata")
        put(CalendarContract.Events.DTSTART, Calendar.getInstance().timeInMillis + 1 * 60 * 1000)
        put(CalendarContract.Events.DTEND, Calendar.getInstance().timeInMillis + 31 * 60 * 1000)
    })

    Log.w("getEventsList", "insert opr ${insertUri.toString()}")
}