package com.dragonfire.wildfirelauncher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

class AppObject {

    private String appname, packagename;
    private Drawable appicon;
    private long usagetime;

    public AppObject(String appname, String packagename, Drawable appicon) {
        this.appname = appname;
        this.packagename = packagename;
        this.appicon = appicon;
    }

    public String getAppname() {
        return appname;
    }

    public String getPackagename() {
        return packagename;
    }

    public Drawable getAppicon() {
        return appicon;
    }

    public Bitmap getIconBitmap() {
        return getBitmapFromDrawable(appicon);
    }

    public void setUsagetime(long usagetime) {
        this.usagetime = usagetime;
    }

    public long getUsagetime() {
        return usagetime;
    }

    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return getCircularBitmap(bmp);
    }

    protected Bitmap getCircularBitmap(Bitmap src) {
        // Calculate the circular bitmap width with border
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0XFF000000);
        int radius = Math.min(src.getWidth()/2, src.getHeight()/2)-10;
        canvas.drawCircle(src.getWidth()/2, src.getHeight()/2, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);

        return output;
    }
}
