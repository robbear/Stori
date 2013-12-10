package com.hyperfine.neodori;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class Utilities {
    public final static String TAG = "Utilities";

    public final static int JSON_INDENT_SPACES = 2;

    public static String getUserUuidString(Context context) {
        if(D)Log.d(TAG, "Utilities.getUserUuidString");

        SharedPreferences prefs = context.getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
        String uuid = prefs.getString(SSPreferences.PREFS_USERUUID, null);

        if(D)Log.d(TAG, String.format("Utilities.getUserUuidString returning %s", uuid));

        return uuid;
    }

    //
    // Creates or gets the SlideShare directory for SlideShare name, slideShareName.
    // This method also sets the SSPreferences.SSNAME to slideShareName.
    //
    public static File createOrGetSlideShareDirectory(Context context, String slideShareName) {
        if(D)Log.d(TAG, String.format("Utilities.createOrGetSlideShareDirectory: dirName=%s", slideShareName));

        SharedPreferences prefs = context.getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        File rootDir = getRootFilesDirectory(context);

        File slideShareDirectory = new File(rootDir.getAbsolutePath() + "/" + slideShareName);
        slideShareDirectory.mkdir();

        Editor editor = prefs.edit();
        editor.putString(SSPreferences.PREFS_SSNAME, slideShareName);
        editor.commit();

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

    public static boolean copyExternalStorageImageToJPG(Context context, String slideShareName, String fileName, String imageFilePath) {
        if(D)Log.d(TAG, String.format("Utilities.copyExternalStorageImageToJPG: slideShareName=%s, fileName=%s, imageFilePath=%s", slideShareName, fileName, imageFilePath));

        boolean success = false;
        OutputStream outStream = null;
        File slideShareDirectory = createOrGetSlideShareDirectory(context, slideShareName);

        if (slideShareDirectory == null) {
            if(D)Log.d(TAG, "Utilities.copyExternalStorageImageToJPG - failed to retrieve slideShareDirectory. Bailing");
            return false;
        }

        try {
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

            File f = new File(imageFilePath);
            Uri uri = Uri.fromFile(f);
            Bitmap bitmapImage = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
            if (bitmapImage.compress(Bitmap.CompressFormat.JPEG, Config.jpgCompressionLevel, outputBuffer)) {
                File file = createFile(context, slideShareName, fileName);
                outStream = new FileOutputStream(file);

                outputBuffer.writeTo(outStream);
                success = true;
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
        }

        return success;
    }

    public static boolean copyGalleryImageToJPG(Context context, String slideShareName, String fileName, Intent intent) {
        if(D)Log.d(TAG, String.format("Utilities.copyGalleryImageToJPG: slideShareName=%s, fileName=%s", slideShareName, fileName));

        boolean success = false;
        OutputStream outStream = null;
        File slideShareDirectory = createOrGetSlideShareDirectory(context, slideShareName);

        if (slideShareDirectory == null) {
            if(D)Log.d(TAG, "Utilities.copyGalleryImageToJPG - failed to retrieve slideShareDirectory. Bailing");
            return false;
        }

        try {
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

            Bitmap bitmapImage = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(intent.getData()));
            if (bitmapImage.compress(Bitmap.CompressFormat.JPEG, Config.jpgCompressionLevel, outputBuffer)) {
                File file = createFile(context, slideShareName, fileName);
                outStream = new FileOutputStream(file);

                outputBuffer.writeTo(outStream);
                success = true;
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

    public static void shareShow(Context context, String userUuid, String slideShareName) {
        if(D)Log.d(TAG, "Utilities.shareShow");

        String appName = context.getString(R.string.app_name);
        String url = Utilities.buildShowWebPageUrlString(userUuid, slideShareName);
        String message = String.format(context.getString(R.string.share_email_body_format), appName, url);
        String subject = String.format(context.getString(R.string.share_email_subject_format), appName);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_dialog_title)));
    }

    public static String buildResourceUrlString(String userUuid, String slideShareName, String fileName) {
        if (fileName == null) {
            return null;
        }

        String urlString = Config.baseCloudUrl + userUuid.toString() + "/" + slideShareName + "/" + fileName;
        if(D)Log.d(TAG, String.format("Utilities.buildResourceUrlString: %s", urlString));

        return urlString;
    }

    public static String buildShowWebPageUrlString(String userUuid, String slideShareName) {
        String urlString = Config.baseWebSlidesUrl + userUuid.toString() + "/" + slideShareName;

        return urlString;
    }
}
