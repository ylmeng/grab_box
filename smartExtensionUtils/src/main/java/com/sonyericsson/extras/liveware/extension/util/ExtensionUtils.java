/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2013-2014 Sony Mobile Communications AB.

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

 * Neither the name of the Sony Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonyericsson.extras.liveware.extension.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.control.Control.Intents;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.registration.Registration.Device;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DisplayColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.VersionColumns;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * The extension utils class contains utility functions used by several
 * extensions.
 */
public class ExtensionUtils {

    /**
     * Invalid id
     */
    public static final int INVALID_ID = -1;

    /**
     * Draw text on canvas. Shade if text too long to fit.
     *
     * @param canvas The canvas to draw in.
     * @param text The text to draw.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param textPaint The paint to draw with.
     * @param availableWidth The available width for the text
     */
    public static void drawText(Canvas canvas, String text, float x, float y, TextPaint textPaint,
            int availableWidth) {
        text = text.replaceAll("\\r?\\n", " ");
        final TextPaint localTextPaint = new TextPaint(textPaint);
        final float pixelsToShade = 1.5F * localTextPaint.getTextSize();
        int characters = text.length();

        if (localTextPaint.measureText(text) > availableWidth) {
            Paint.Align align = localTextPaint.getTextAlign();
            float shaderStopX;
            characters = localTextPaint.breakText(text, true, availableWidth, null);
            if (align == Paint.Align.LEFT) {
                shaderStopX = x + availableWidth;
            } else if (align == Paint.Align.CENTER) {
                float[] measuredWidth = new float[1];
                characters = localTextPaint.breakText(text, true, availableWidth, measuredWidth);
                shaderStopX = x + (measuredWidth[0] / 2);
            } else { // align == Paint.Align.RIGHT
                shaderStopX = x;
            }
            // Hex 0x60000000 = first two bytes is alpha, gives semitransparent
            localTextPaint.setShader(new LinearGradient(shaderStopX - pixelsToShade, 0,
                    shaderStopX, 0, localTextPaint.getColor(),
                    localTextPaint.getColor() + 0x60000000, Shader.TileMode.CLAMP));
        }
        canvas.drawText(text, 0, characters, x, y, localTextPaint);
    }

    /**
     * Get URI string from resourceId.
     *
     * @param context The context.
     * @param resourceId The resource id.
     * @return The URI string.
     */
    public static String getUriString(final Context context, final int resourceId) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName()).appendPath(Integer.toString(resourceId))
                .toString();
    }

    /**
     * Check in the database if there are any accessories connected.
     *
     * @param context The context
     * @return True if at least one accessories is connected.
     */
    public static boolean areAnyAccessoriesConnected(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Device.URI, null,
                    DeviceColumns.ACCESSORY_CONNECTED + " = 1", null, null);
            if (cursor != null) {
                return (cursor.getCount() > 0);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Get the contact name from a URI.
     *
     * @param context The context.
     * @param contactUri The contact URI.
     *
     * @return The contact name.
     */
    public static String getContactName(final Context context, Uri contactUri) {
        String name = null;
        if (contactUri != null) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(contactUri, new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME
                }, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor
                            .getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }

            }
        }

        return name;
    }

    /**
     * Get the contact photo from a contact URI.
     *
     * @param context The context.
     * @param contactUri The contact URI.
     *
     * @return The contact photo.
     */
    public static Bitmap getContactPhoto(final Context context, Uri contactUri) {
        Bitmap bitmap = null;
        if (contactUri != null) {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    context.getContentResolver(), contactUri);
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
        }

        return bitmap;
    }

    /**
     * Get bitmap from a URI.
     *
     * @param context The context.
     * @param uriString The URI as a string.
     *
     * @return The bitmap.
     */
    public static Bitmap getBitmapFromUri(final Context context, String uriString) {
        Bitmap bitmap = null;
        if (uriString == null) {
            return null;
        }

        Uri uri = Uri.parse(uriString);
        if (uri != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                if (bitmap != null) {
                    // We use default density for all bitmaps to avoid scaling.
                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                }
            } catch (IOException e) {

            }
        }
        return bitmap;
    }

    /**
     * Get id of a registered extension
     *
     * @return Id, {@link #INVALID_ID} if extension is not registered
     */
    public static long getExtensionId(Context context) {
        Cursor cursor = null;
        long id = INVALID_ID;
        String selection = Registration.ExtensionColumns.PACKAGE_NAME + " = ?";
        String[] selectionArgs = new String[] {
            context.getPackageName()
        };
        try {
            cursor = context.getContentResolver().query(Registration.Extension.URI, null,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Registration.ExtensionColumns._ID);
                id = cursor.getLong(idIndex);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * Get id of a registered extension
     *
     * @return Id, {@link #INVALID_ID} if extension is not registered
     */
    public static long getRegistrationId(Context context, String hostAppPackageName, long extensionId) {
        Cursor cursor = null;
        long id = INVALID_ID;
        String selection = Registration.ApiRegistrationColumns.HOST_APPLICATION_PACKAGE
                + " = ? AND " + Registration.ApiRegistrationColumns.EXTENSION_ID + " = ?";
        String[] selectionArgs = new String[] {
                hostAppPackageName, Long.toString(extensionId)
        };
        try {
            cursor = context.getContentResolver().query(Registration.ApiRegistration.URI, null,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Registration.ApiRegistrationColumns._ID);
                id = cursor.getLong(idIndex);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * Get the value of the intent extra parameter
     * {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_HISTORY} from the
     * intent that started the configuration activity.
     *
     * @param intent The intent that started the configuration activity, see
     *            {@link Registration.ExtensionColumns#CONFIGURATION_ACTIVITY}
     * @return Value of
     *         {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_HISTORY},
     *         true if not contained in the intent extras.
     */
    public static boolean supportsHistory(Intent intent) {
        boolean supportsHistory = true;
        if (intent == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: intent == null");
            }
            return supportsHistory;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: extras == null");
            }
            return supportsHistory;
        }
        if (extras.containsKey(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_HISTORY)) {
            supportsHistory = extras
                    .getBoolean(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_HISTORY);
        } else {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: EXTRA_ACCESSORY_SUPPORTS_HISTORY not present");
            }
        }
        return supportsHistory;
    }

    /**
     * Get the value of the intent extra parameter
     * {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_ACTIONS} from the
     * intent that started the configuration activity.
     *
     * @param intent The intent that started the configuration activity, see
     *            {@link Registration.ExtensionColumns#CONFIGURATION_ACTIVITY}
     * @return Value of
     *         {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_ACTIONS},
     *         true if not contained in the intent extras.
     */
    public static boolean supportsActions(Intent intent) {
        boolean supportsActions = true;
        if (intent == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: intent == null");
            }
            return supportsActions;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: extras == null");
            }
            return supportsActions;
        }
        if (extras.containsKey(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_ACTIONS)) {
            supportsActions = extras
                    .getBoolean(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_ACTIONS);
        } else {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: EXTRA_ACCESSORY_SUPPORTS_ACTIONS not present");
            }
        }
        return supportsActions;
    }

    /**
     * Get formatted time.
     *
     * @param publishedTime The published time in millis.
     *
     * @return The formatted time.
     */
    static public String getFormattedTime(long publishedTime) {
        // This is copied from RecentCallsListActivity.java

        long now = System.currentTimeMillis();

        // Set the date/time field by mixing relative and absolute times.
        int flags = DateUtils.FORMAT_ABBREV_ALL;

        if (!DateUtils.isToday(publishedTime)) {
            // DateUtils.getRelativeTimeSpanString doesn't consider the nature
            // days comparing with DateUtils.getRelativeDayString. Override the
            // real date to implement the requirement.

            Time time = new Time();
            time.set(now);
            long gmtOff = time.gmtoff;
            int days = Time.getJulianDay(publishedTime, gmtOff) - Time.getJulianDay(now, gmtOff);

            // Set the delta from now to get the correct display
            publishedTime = now + days * DateUtils.DAY_IN_MILLIS;
        } else if (publishedTime > now && (publishedTime - now) < DateUtils.HOUR_IN_MILLIS) {
            // Avoid e.g. "1 minute left" when publish time is "07:00" and
            // current time is "06:58"
            publishedTime += DateUtils.MINUTE_IN_MILLIS;
        }

        return (DateUtils.getRelativeTimeSpanString(publishedTime, now, DateUtils.MINUTE_IN_MILLIS,
                flags)).toString();
    }

    /**
     * Get the version of the registration API that is implemented by the
     * current phone/tablet the extension is running on.
     *
     * @param context The context.
     * @return The version of the registration API.
     */
    public static int getRegistrationVersion(Context context) {
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(Registration.Version.URI, null, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(VersionColumns.VERSION));
            }
        } catch (SQLException e) {
            // Expected on V2 or V1 where version table is missing.
            if (ExtensionUtils.hasIsEmulatedColumnInDisplayTable(context)) {
                return 2;
            } else {
                return 1;
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query version", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query version", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 1;
    }

    /**
     * Check if the
     * {@link com.sonyericsson.extras.liveware.aef.registration.Registration.DisplayColumns#IS_EMULATED}
     * column is present in the
     * {@link com.sonyericsson.extras.liveware.aef.registration.Registration.Display}
     * table in the registration API.
     *
     * @param context The context.
     * @return True if the table exists.
     */
    public static boolean hasIsEmulatedColumnInDisplayTable(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Registration.Display.URI, null, null, null,
                    null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndex(DisplayColumns.IS_EMULATED);
                return columnIndex != -1;
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query display", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query display", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query display", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    private static enum WhiteList {

        // TODO: add signature
        /**
         * The Smart Eyeglass host application is signed with a
         * different key, but should still have host app privileges
         */

        EYEGLASS_HOST_APP("com.sony.smarteyeglass", new byte[] {
            0, 92, -67, -10, 5, -69, 21, 34, -105, 3, 69, 83, -122, 19, 50, 43,
            28, 70, 90, -49, -72, 77, -20, -112, -30, 90, -37, -24, -14, 46, -36,
            -55
        });
        private String mPkgName;

        private byte[] mHashedSignature;

        WhiteList(String pkg, byte[] signature) {
            mPkgName = pkg;
            mHashedSignature = signature;
        }

        public static boolean exists(String pkgName, Signature signature) {
            for (WhiteList element : WhiteList.values()) {
                if (element.mPkgName.equals(pkgName)) {
                    byte[] hashed = getHashedSignature(signature.toByteArray());
                    if (hashed != null && Arrays.equals(element.mHashedSignature, hashed)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static byte[] getHashedSignature(byte[] original) {
            try {
                MessageDigest digester = MessageDigest.getInstance("SHA-256");
                return digester.digest(original);
            } catch (NoSuchAlgorithmException e) {
                Dbg.e("Unable to transform signature.");
            }
            return null;
        }
    }

    /**
     * Send intent to host application. Adds host application package name and
     * our package name.
     *
     * @param context a context
     * @param hostPkgName package name of the host app
     * @param intent The intent to send.
     */
    public static void sendToHostApp(Context context, String hostPkgName, Intent intent) {
        boolean whiteListed = false;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(hostPkgName,
                    PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            whiteListed = WhiteList.exists(hostPkgName, signatures[0]);
        } catch (NameNotFoundException e) {
            Dbg.e("No package info for host app", e);
            return;
        }
        intent.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, context.getPackageName());
        intent.setPackage(hostPkgName);
        if (whiteListed) {
            context.sendBroadcast(intent);
        } else {
            context.sendBroadcast(intent, Registration.HOSTAPP_PERMISSION);
        }
    }
}
