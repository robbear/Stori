package com.stori.stori;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.stori.stori.cloudproviders.AmazonSharedPreferencesWrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class Utilities {
    public final static String TAG = "Utilities";

    public final static int JSON_INDENT_SPACES = 2;

    public static String getUserUuidString(Context context) {
        if(D)Log.d(TAG, "Utilities.getUserUuidString");

        SharedPreferences prefs = context.getSharedPreferences(SSPreferences.PREFS(context), Context.MODE_PRIVATE);
        String uuid = AmazonSharedPreferencesWrapper.getUsername(prefs);

        if(D)Log.d(TAG, String.format("Utilities.getUserUuidString returning %s", uuid));

        return uuid;
    }

    public static void clearAllData(Context context, SharedPreferences prefs) {
        if(D)Log.d(TAG, "Utilities.clearAllData");

        EditPlayActivity.s_amazonClientManager.clearCredentials();
        EditPlayActivity.s_amazonClientManager.wipe();

        String slideShareName = prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(context), null);
        if (slideShareName != null) {
            if(D)Log.d(TAG, String.format("Utilities.clearAllData - deleting PlaySlides directory: %s", slideShareName));
            Utilities.deleteSlideShareDirectory(context, slideShareName);
        }

        slideShareName = prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(context), null);
        if (slideShareName != null) {
            if(D)Log.d(TAG, String.format("Utilities.clearAllData - deleting EditSlides directory: %s", slideShareName));
            Utilities.deleteSlideShareDirectory(context, slideShareName);
        }

        if(D)Log.d(TAG, "Utilities.clearAllData - clearing PREFS_PLAYSLIDESNAME and PREFS_EDITPROJECTNAME");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SSPreferences.PREFS_PLAYSLIDESNAME(context), null);
        editor.putString(SSPreferences.PREFS_EDITPROJECTNAME(context), null);
        editor.commit();
    }

    //
    // Creates or gets the SlideShare directory for SlideShare name, slideShareName.
    //
    public static File createOrGetSlideShareDirectory(Context context, String slideShareName) {
        if(D)Log.d(TAG, String.format("Utilities.createOrGetSlideShareDirectory: dirName=%s", slideShareName));

        File rootDir = getRootFilesDirectory(context);

        File slideShareDirectory = new File(rootDir.getAbsolutePath() + "/" + slideShareName);
        slideShareDirectory.mkdir();

        return slideShareDirectory;
    }

    private static void recursiveDelete(File f) {
        if(D)Log.d(TAG, String.format("recursiveDelete: %s", f.getAbsolutePath()));
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                recursiveDelete(file);
            }
        }

        boolean retVal = f.delete();
        if(D)Log.d(TAG, String.format("recursiveDelete f.delete() returns %b for %s", retVal, f.getAbsolutePath()));
    }

    public static void deleteSlideShareDirectory(Context context, String slideShareName) {
        if(D)Log.d(TAG, String.format("Utilities.deleteSlideShareDirectory: dirName=%s", slideShareName));

        File rootDir = getRootFilesDirectory(context);

        File slideShareDirectory = new File(rootDir.getAbsolutePath() + "/" + slideShareName);
        if (slideShareDirectory.exists()) {
            recursiveDelete(slideShareDirectory);

            if(D)Log.d(TAG, String.format("Utilities.deleteSlideShareDirectory: slideShareDirectory.exists() returns %b", slideShareDirectory.exists()));
        }
    }

    private static File createFile(File directory, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.createFile: directory=%s, fileName=%s", directory.getAbsolutePath(), fileName));

        File file = null;

        try {
            file = new File(directory.getAbsolutePath() + "/" + fileName);
            if (file.exists()) {
                if(D)Log.d(TAG, "Utilities.createFile - file exists, so deleting it first");
                file.delete();
            }
            file.createNewFile();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.createFile", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.createFile", e);
            e.printStackTrace();
        }

        return file;
    }

    public static File createFile(Context context, String directoryName, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.createFile: directoryName=%s, fileName=%s", directoryName, fileName));

        File directory = createOrGetSlideShareDirectory(context, directoryName);
        if (directory == null) {
            if(D)Log.d(TAG, "Utilities.createFile - createOrGetSlideShareDirectory returned null. Bailing.");
            return null;
        }

        return createFile(directory, fileName);
    }

    public static boolean deleteFile(Context context, String folder, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.deleteFile: folder=%s, fileName=%s", folder, fileName));

        boolean success = true;

        File dirRoot = getRootFilesDirectory(context);
        File directory = new File(dirRoot.getAbsolutePath() + "/" + folder);
        if (directory.exists() && directory.isDirectory()) {
            File file = new File(directory, fileName);
            if (file.exists()) {
                success = file.delete();
            }
        }

        if(D)Log.d(TAG, String.format("Utilities.deleteFile returns: %b", success));
        return success;
    }

    public static boolean saveStringToFile(Context context, String data, String folder, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.saveStringToFile: folder=%s, fileName=%s", folder, fileName));

        boolean retVal = false;

        File dirRoot = getRootFilesDirectory(context);
        File directory = new File(dirRoot.getAbsolutePath() + "/" + folder);
        File file = new File(directory, fileName);

        FileOutputStream fos = null;

        try {
            file.createNewFile();
            if(D)Log.d(TAG, String.format("Utilities.saveStringToFile: file=%s", file.getAbsolutePath()));
            fos = new FileOutputStream(file);
            fos.write(data.getBytes());
            retVal = true;
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.saveStringToFile", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.saveStringToFile", e);
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            }
            catch (Exception e) {}
        }

        return retVal;
    }

    public static String loadStringFromFile(Context context, String folder, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.loadStringFromFile: folder=%s, fileName=%s", folder, fileName));

        FileInputStream fis = null;
        String data = null;

        File dirRoot = getRootFilesDirectory(context);
        File directory = new File(dirRoot.getAbsolutePath() + "/" + folder);
        if (directory.exists() && directory.isDirectory()) {
            File file = new File(directory, fileName);
            if (file.exists()) {
                try {
                    byte[] buffer = new byte[(int)file.length()];
                    fis = new FileInputStream(file);
                    fis.read(buffer);

                    data = new String(buffer, "UTF-8");
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "Utilities.loadStringFromFile", e);
                    e.printStackTrace();
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "Utilities.loadStringFromFile", e);
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    }
                    catch (Exception e) {}
                }
            }
            else {
                if(D)Log.d(TAG, "Utilities.loadStringFromFile - file doesn't exist. Bailing.");
            }
        }
        else {
            if(D)Log.d(TAG, "Utilities.loadStringFromFile - folder doesn't exist. Bailing.");
        }

        return data;
    }

    public static String getAbsoluteFilePath(Context context, String folder, String fileName) {
        if(D)Log.d(TAG, String.format("Utilities.getAbsoluteFilePath: folder=%s, fileName=%s", folder, fileName));

        File dir = getRootFilesDirectory(context);
        String path = dir.getAbsolutePath() + "/" + folder + "/" + fileName;
        if(D)Log.d(TAG, String.format("Utilities.getAbsoluteFilePath returning %s", path));
        return path;
    }

    public static File getRootFilesDirectory(Context context) {
        if(D)Log.d(TAG, "Utilities.getRootFilesDirectory");

        File dir = null;
        if (Config.USE_CACHE) {
            dir = context.getCacheDir();
        }
        else {
            dir = context.getFilesDir();
        }

        if(D)Log.d(TAG, String.format("Utilities.getRootFilesDirectory - returning %s", dir.getAbsolutePath()));

        return dir;
    }

    public static void listAllFilesAndDirectories(Context context, File dir) {
        if(D)Log.d(TAG, String.format("Utilities.listAllFilesAndDirectories for %s", dir == null ? "null" : dir));

        ArrayList<File> directories = new ArrayList<File>();

        if (dir == null) {
            dir = getRootFilesDirectory(context);
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String file = files[i].getAbsolutePath();
                if(D)Log.d(TAG, String.format("Utilities.listAllFilesAndDirectories - file: %s, isDirectory=%b, size=%d", file, files[i].isDirectory(), files[i].length()));

                if (files[i].isDirectory()) {
                    directories.add(files[i]);
                }
            }
        }

        for (int i = 0; i < directories.size(); i++) {
            listAllFilesAndDirectories(context, directories.get(i));
        }
    }

    private static int calculateInSampleSize(int bitmapWidth, int bitmapHeight, int displayWidth, int displayHeight) {
        if(D)Log.d(TAG, String.format("Utilities.calculateInSampleSize: bW=%d, bH=%d, dW=%d, dH=%d", bitmapWidth, bitmapHeight, displayWidth, displayHeight));

        if (displayWidth == 0 || displayHeight == 0) {
            return 1;
        }

        int scaleFactor = Math.max(bitmapWidth / displayWidth, bitmapHeight / displayHeight);

        return Math.max(1, scaleFactor);
    }

    private static Bitmap rotateBitmap(String filePath, Bitmap bitmap) {
        if(D)Log.d(TAG, String.format("Utilities.rotateBitmap for %s", filePath));

        if (bitmap == null) {
            return null;
        }

        // See: https://gist.github.com/9re/1990019

        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            if(D)Log.d(TAG, String.format("Utilities.rotateBitmap: orientation=%d", orientation));

            Matrix matrix = new Matrix();
            switch (orientation) {
                //2
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                //3
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                //4
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                //5
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                //6
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                //7
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                //8
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                default:
                    return bitmap;
            }

            if(D)Log.d(TAG, "Utilities.rotateBitmap - creating a rotated bitmap");
            Bitmap bitmapOriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return bitmapOriented;
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.rotateBitmap", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.rotateBitmap", e);
            e.printStackTrace();
        }

        return bitmap;
    }

    public static Bitmap getConstrainedBitmap(String filePath) {
        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap for file copy: filePath=%s", filePath));

        long bitmapSize = 0;
        File file = new File(filePath);
        if (file.exists()) {
            bitmapSize = file.length();
            if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: length %d for file %s", bitmapSize, filePath));
        }
        else {
            if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: %s does not exist. Bailing.", filePath));
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int bitmapWidth = options.outWidth;
        int bitmapHeight = options.outHeight;
        int scaleFactor = 1;
        boolean isLandscape = bitmapWidth > bitmapHeight;

        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: bitmapWidth=%d, bitmapHeight=%d", bitmapWidth, bitmapHeight));

        if (bitmapSize > Config.imageFileSizeFloorBytes) {
            scaleFactor = calculateInSampleSize(
                    bitmapWidth, bitmapHeight,
                    isLandscape ? Config.imageDisplayWidthLandcape : Config.imageDisplayWidthPortrait,
                    isLandscape ? Config.imageDisplayHeightLandscape : Config.imageDisplayHeightPortrait);
        }
        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: scaleFactor=%d", scaleFactor));

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;
        options.inPurgeable = true;

        if(D)Log.d(TAG, "Utilities.getConstrainedBitmap - returning bitmap from decodeFile");
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        bitmap = rotateBitmap(filePath, bitmap);
        if(D)Log.d(TAG, String.format("Utililties.getConstrained/RotatedBitmap: bitmap %s null", bitmap == null ? "is" : "is not"));

        return bitmap;
    }

    public static Bitmap getConstrainedBitmap(String filePath, int displayWidth, int displayHeight) {
        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: displayWidth=%d, displayHeight=%d, filePath=%s", displayWidth, displayHeight, filePath));

        long bitmapSize = 0;
        File file = new File(filePath);
        if (file.exists()) {
            bitmapSize = file.length();
            if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: length %d for file %s", bitmapSize, filePath));
        }
        else {
            if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: %s does not exist. Bailing.", filePath));
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int bitmapWidth = options.outWidth;
        int bitmapHeight = options.outHeight;
        int scaleFactor = 1;
        boolean isLandscape = bitmapWidth > bitmapHeight;

        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: bitmapWidth=%d, bitmapHeight=%d", bitmapWidth, bitmapHeight));

        if (displayWidth == 0 && displayHeight == 0) {
            if (isLandscape) {
                displayWidth = Config.imageDisplayWidthLandcape;
                displayHeight = Config.imageDisplayHeightLandscape;
            }
            else {
                displayWidth = Config.imageDisplayWidthPortrait;
                displayHeight = Config.imageDisplayHeightPortrait;
            }
        }
        else {
            if (displayWidth == 0) {
                displayWidth = (bitmapWidth * displayHeight) / bitmapHeight;
            }
            else if (displayHeight == 0) {
                displayHeight = (bitmapHeight * displayWidth) / bitmapWidth;
            }
        }

        scaleFactor = calculateInSampleSize(bitmapWidth, bitmapHeight, displayWidth, displayHeight);
        if(D)Log.d(TAG, String.format("Utilities.getConstrainedBitmap: scaleFactor=%d", scaleFactor));

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;
        options.inPurgeable = true;

        if(D)Log.d(TAG, "Utilities.getConstrainedBitmap - returning bitmap from decodeFile");
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        bitmap = rotateBitmap(filePath, bitmap);
        if(D)Log.d(TAG, String.format("Utililties.getConstrained/RotatedBitmap: bitmap %s null", bitmap == null ? "is" : "is not"));

        return bitmap;
    }

    public static boolean copyExternalStorageImageToJPG(Context context, String slideShareName, String fileName, String imageFilePath) {
        if(D)Log.d(TAG, String.format("Utilities.copyExternalStorageImageToJPG: slideShareName=%s, fileName=%s, imageFilePath=%s", slideShareName, fileName, imageFilePath));

        boolean success = false;
        OutputStream outStream = null;
        InputStream is = null;
        File slideShareDirectory = createOrGetSlideShareDirectory(context, slideShareName);

        if (slideShareDirectory == null) {
            if(D)Log.d(TAG, "Utilities.copyExternalStorageImageToJPG - failed to retrieve slideShareDirectory. Bailing");
            return false;
        }

        try {
            File f = new File(imageFilePath);
            Uri uri = Uri.fromFile(f);
            is = context.getContentResolver().openInputStream(uri);

            File file = createFile(context, slideShareName, fileName);
            outStream = new FileOutputStream(file);

            // Copy the bitmap file to disk
            byte[] buffer = new byte[10240];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
            outStream.close();
            outStream = null;

            if (D) {
                if (file.exists()) {
                    if(D)Log.d(TAG, String.format("Utilities.copyExternalStorageImageToJPG: original file length is %d", file.length()));
                }
            }

            // Now compress the file
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            Bitmap bitmap = getConstrainedBitmap(Utilities.getAbsoluteFilePath(context, slideShareName, fileName));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputBuffer)) {
                file = createFile(context, slideShareName, fileName);
                outStream = new FileOutputStream(file);

                outputBuffer.writeTo(outStream);
                success = true;

                if (D) {
                    if (file.exists()) {
                        if(D)Log.d(TAG, String.format("Utilities.copyExternalStorageImageToJPG: compressed file length is %d", file.length()));
                    }
                }
            }
            else {
                if(D)Log.d(TAG, "Utilities.copyExternalStorageImageToJPG failed");
            }
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "Utilities.copyExternalStorageImageToJPG", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.copyExternalStorageImageToJPG", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.copyExternalStorageImageToJPG", e);
            e.printStackTrace();
        }
        finally {
            if (outStream != null) {
                try {
                    outStream.close();
                }
                catch (Exception e) {}
            }

            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception e) {}
            }
        }

        return success;
    }

    public static boolean copyGalleryImageToJPG(Context context, String slideShareName, String fileName, Uri uri) {
        if(D)Log.d(TAG, String.format("Utilities.copyGalleryImageToJPG: slideShareName=%s, fileName=%s", slideShareName, fileName));

        boolean success = false;
        OutputStream outStream = null;
        InputStream is = null;
        File slideShareDirectory = createOrGetSlideShareDirectory(context, slideShareName);

        if (slideShareDirectory == null) {
            if(D)Log.d(TAG, "Utilities.copyGalleryImageToJPG - failed to retrieve slideShareDirectory. Bailing");
            return false;
        }

        try {
            is = context.getContentResolver().openInputStream(uri);

            File file = createFile(context, slideShareName, fileName);
            outStream = new FileOutputStream(file);

            // Copy the bitmap file to disk
            byte[] buffer = new byte[10240];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
            outStream.close();
            outStream = null;

            if (D) {
                if (file.exists()) {
                    if(D)Log.d(TAG, String.format("Utilities.copyGalleryImageToJPG: original file length is %d", file.length()));
                }
            }

            // Now compress the file
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            Bitmap bitmap = getConstrainedBitmap(Utilities.getAbsoluteFilePath(context, slideShareName, fileName));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputBuffer)) {
                file = createFile(context, slideShareName, fileName);
                outStream = new FileOutputStream(file);

                outputBuffer.writeTo(outStream);
                success = true;

                if (D) {
                    if (file.exists()) {
                        if(D)Log.d(TAG, String.format("Utilities.copyGalleryImageToJPG: compressed file length is %d", file.length()));
                    }
                }
            }
            else {
                if(D)Log.d(TAG, "Utilities.copyGalleryImageToJPG failed");
            }
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "Utilities.copyGalleryImageToJPG", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.copyGalleryImageToJPG", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.copyGalleryImageToJPG", e);
            e.printStackTrace();
        }
        finally {
            if (outStream != null) {
                try {
                    outStream.close();
                }
                catch (Exception e) {}
            }

            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception e) {}
            }
        }

        return success;
    }

    public static boolean copyFile(File src, File dest) {
        if(D)Log.d(TAG, String.format("Utilities.copyFile: src=%s, dest=%s", src.getAbsolutePath(), dest.getAbsolutePath()));

        boolean success = false;
        InputStream is = null;
        OutputStream os = null;

        try {
            dest.createNewFile();

            is = new FileInputStream(src);
            os = new FileOutputStream(dest);

            byte[] buffer = new byte[10240];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }

            success = true;
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "Utilities.copyFile", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "Utilities.copyFile", e);
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception e) {}
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (Exception e) {}
            }
        }

        if (!success) {
            if (dest.exists()) {
                dest.delete();
            }
        }

        return success;
    }

    public static void printSlideShareJSON(String tag, SlideShareJSON ssj) {
        try {
            if(D)Log.d(tag, ssj.toString(JSON_INDENT_SPACES));
        }
        catch (Exception e) {
            if(E)Log.e(tag, "Utilities.printSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(tag, "Utilities.printSlideShareJSON", e);
            e.printStackTrace();
        }
    }

    public static boolean isCameraAvailable(Context context) {
        if(D)Log.d(TAG, "Utilities.isCameraAvailable");

        final PackageManager pm = context.getPackageManager();

        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return false;
        }

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

    public static void shareShow(Context context, String userUuid, String slideShareName, String title) {
        if(D)Log.d(TAG, "Utilities.shareShow");

        String appName = context.getString(R.string.app_name);
        String url = Utilities.buildShowWebPageUrlString(userUuid, slideShareName);
        String marketUrl = context.getString(R.string.google_play_market_link);
        String message = String.format(context.getString(R.string.share_email_body_format), appName, title, url, marketUrl);
        String subject = String.format(context.getString(R.string.share_email_subject_format), appName, title);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_dialog_title)));
    }

    public static boolean deviceHasHardwareKeyboard(Context context) {
        boolean hasKeyboard = false;

        Configuration config = context.getResources().getConfiguration();
        hasKeyboard = config.keyboard != Configuration.KEYBOARD_NOKEYS;

        if(D)Log.d(TAG, String.format("Utilities.devieHasHardwareKeyboard: returning %b", hasKeyboard));

        return hasKeyboard;
    }

    // See: https://groups.google.com/a/openmrs.org/forum/#!topic/dev/XlzO0KP-rUo
    // Convert format YYYY-MM-DDThh:mm:ss.sssZ to YYYY-MM-DDThh:mm:ss+00:00
    public static Calendar toCalendarFromISO8601String(String iso8601String) throws ParseException {
        if(D)Log.d(TAG, String.format("Utilities.toCalendarFromISO8601String: %s", iso8601String));

        Calendar calendar = GregorianCalendar.getInstance();

        String s = iso8601String.substring(0, 19) + "+00:00";
        if(D)Log.d(TAG, String.format("Utilities.toCalendarFromISO8601String: s=%s", s));

        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
        calendar.setTime(date);

        return calendar;
    }

    public static void freezeActivityOrientation(Activity activity) {
        if(D)Log.d(TAG, "Utilities.freezeOrientation");

        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public static void unfreezeOrientation(Activity activity) {
        if(D)Log.d(TAG, "Utilities.unfreezeOrientation");

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    public static String buildResourceUrlString(String userUuid, String slideShareName, String fileName) {
        if (fileName == null || fileName.length() == 0) {
            if(D)Log.d(TAG, "Utilities.buildResourceUrlString - returning null");
            return null;
        }

        String urlString = Config.baseCloudUrl + userUuid + "/" + slideShareName + "/" + fileName;
        if(D)Log.d(TAG, String.format("Utilities.buildResourceUrlString: %s", urlString));

        return urlString;
    }

    public static String buildShowWebPageUrlString(String userUuid, String slideShareName) {
        String urlString = Config.baseWebSlidesUrl + userUuid + "/" + slideShareName;

        return urlString;
    }
}
