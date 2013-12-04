package com.npi.blureffect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ScrollableImageView extends ImageView {

	// A Paint object used to render the image
	private Paint paint = new Paint();
	// The original Bitmap
	private Bitmap originalImage;
	// The screen width used to render the image
	private int screenWidth;
	private int offset;

    private Matrix mMatrix;

	public ScrollableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ScrollableImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollableImageView(Context context) {
		this(context, null);
	}

	/**
	 * Draws the view if the adapted image is not null
	 */
	@Override
	protected void onDraw(Canvas canvas) {
        if (originalImage != null && mMatrix != null)
            canvas.drawBitmap(originalImage, mMatrix, paint);
	}

	public void handleScroll() {

		if (getHeight() > 0 && originalImage != null) {

            int dwidth = originalImage.getWidth();
            int dheight = originalImage.getHeight();

            int vwidth = getWidth();
            int vheight = getHeight();

            mMatrix = new Matrix();

            float scale;
            float dx = 0, dy = 0;

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            mMatrix.setScale(scale, scale);
            mMatrix.postTranslate((int) (dx + 0.5f), (int) (dy - offset + 0.5f));

			invalidate();
		}

	}

    @Override
    public void setImageBitmap(Bitmap bmp) {
        this.originalImage = bmp;
        handleScroll();
    }

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

    public void setOffset(int offset) {
        this.offset = offset;
        handleScroll();
    }
}