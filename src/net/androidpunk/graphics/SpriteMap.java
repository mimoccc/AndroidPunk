package net.androidpunk.graphics;

import java.util.HashMap;
import java.util.Map;

import net.androidpunk.FP;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

/**
 * Performance-optimized animated Image. Can have multiple animations,
 * which draw frames from the provided source image to the screen.
 */
public class SpriteMap extends Image {
	private static final String TAG = "SpriteMap";
	
	public abstract static class OnAnimationEndCallback {
		public abstract void onAnimationEnd();
	}
	
	/**
	 * If the animation has stopped.
	 */
	public boolean complete = true;

	/**
	 * Optional callback function for animation end.
	 */
	public OnAnimationEndCallback callback;

	/**
	 * Animation speed factor, alter this to speed up/slow down all animations.
	 */
	public float rate = 1;
	
	// Spritemap information.
	protected int mWidth;
	protected int mHeight;
	private int mColumns;
	private int mRows;
	private int mFrameCount;
	private final Map<String, Anim> mAnims = new HashMap<String, Anim>();
	private Anim mAnim;
	private int mIndex;
	protected int mFrame;
	private float mTimer = 0;
	
	
	/**
	 * Constructor. frame width = frame height = 0 and no callback.
	 * @param	source			Source image.
	 */
	public SpriteMap(Bitmap source) {
		this(source, 0, 0, null);
	}
	
	/**
	 * Constructor. No callback
	 * @param	source			Source image.
	 * @param	frameWidth		Frame width.
	 * @param	frameHeight		Frame height.
	 */
	public SpriteMap(Bitmap source, int frameWidth, int frameHeight) {
		this(source, frameWidth, frameHeight, null);
	}
	
	/**
	 * Constructor.
	 * @param	source			Source image.
	 * @param	frameWidth		Frame width.
	 * @param	frameHeight		Frame height.
	 * @param	callback		Optional callback function for animation end.
	 */
	public SpriteMap(Bitmap source, int frameWidth, int frameHeight, OnAnimationEndCallback callback) {
		super(source, new Rect(0, 0, frameWidth, frameHeight));
		Rect clipRect = getClipRect();
		
		if (frameWidth == 0)
			clipRect.right = source.getWidth();
		if (frameHeight == 0)
			clipRect.bottom = source.getHeight();
		
		mWidth = source.getWidth();
		mHeight = source.getHeight();
		mColumns = mWidth / clipRect.width();
		mRows = mHeight / clipRect.height();
		mFrameCount = mColumns * mRows;
		this.callback = callback;
		updateBuffer();
		active = true;
	}
	
	/**
	 * Updates the spritemap's buffer without clearing.
	 */
	public void updateBuffer() {
		updateBuffer(true);
	}
	
	/**
	 * Updates the spritemap's buffer.
	 */
	@Override 
	public void updateBuffer(boolean clearBefore) {
		// get position of the current frame
		Rect clipRect = getClipRect();
		int newX = (clipRect.width() * mFrame);
		
		// Happens in constructor call which does not have mWidth member.
		int width = mSource.getWidth();
		
		if (!mFlipped) {
			clipRect.offsetTo(newX % width, (int)((int)(newX / width) * clipRect.height()));
		} else { 
			clipRect.offsetTo((width - clipRect.width()) - (newX % width), (int)(newX * clipRect.height()));
		}

		//Log.d(TAG, String.format("Clipped to %s, frame %d", clipRect.toShortString(), mIndex));
		// update the buffer
		super.updateBuffer(clearBefore);
	}
	
	/** @private Updates the animation. */
	@Override 
	public void update() {
		if (mAnim != null && !complete) {
			mTimer += (FP.fixed ? mAnim.mFrameRate : mAnim.mFrameRate * FP.elapsed) * rate;
			if (mTimer >= 1) {
				while (mTimer >= 1) {
					mTimer -= 1;
					mIndex++;
					if (mIndex == mAnim.mFrameCount) {
						if (mAnim.mLoop) {
							mIndex = 0;
							if (callback != null) 
								callback.onAnimationEnd();
						} else {
							mIndex = mAnim.mFrameCount - 1;
							complete = true;
							if (callback != null)
								callback.onAnimationEnd();
							break;
						}
					}
				}
				if (mAnim != null)
					mFrame = (int)(mAnim.mFrames[mIndex]);
				updateBuffer();
			}
		}
	}
	
	/**
	 * Add an Animation with no change.
	 * @param	name		Name of the animation.
	 * @param	frames		Array of frame indices to animate through.
	 * @return	A new Anim object for the animation.
	 */
	public Anim add(String name, int[] frames) {
		return add(name, frames, 0, true);
	}
	
	/**
	 * Add a looping Animation.
	 * @param	name		Name of the animation.
	 * @param	frames		Array of frame indices to animate through.
	 * @param	frameRate	Animation speed.
	 * @return	A new Anim object for the animation.
	 */
	public Anim add(String name, int[] frames, int frameRate) {
		return add(name, frames, frameRate, true);
	}
	
	/**
	 * Add an Animation.
	 * @param	name		Name of the animation.
	 * @param	frames		Array of frame indices to animate through.
	 * @param	frameRate	Animation speed.
	 * @param	loop		If the animation should loop.
	 * @return	A new Anim object for the animation.
	 */
	public Anim add(String name, int[] frames, int frameRate, boolean loop) {
		if (mAnims.get(name) != null) {
			Log.e(TAG, "Cannot have multiple animations with the same name");
			
		}
		Anim a = new Anim(name, frames, frameRate, loop);
		a.mParent = this;
		mAnims.put(name, a);
		return a;
	}
	
	/**
	 * Plays an animation, without restarting it if already playing.
	 * @param	name		Name of the animation to play.
	 * @return	Anim object representing the played animation.
	 */
	public Anim play(String name) {
		return play(name, false);
	}
	/**
	 * Plays an animation.
	 * @param	name		Name of the animation to play.
	 * @param	reset		If the animation should force-restart if it is already playing.
	 * @return	Anim object representing the played animation.
	 */
	public Anim play(String name, boolean reset) {
		if (!reset && mAnim != null && mAnim.mName.equals(name))
				return mAnim;
		mAnim = mAnims.get(name);
		if (mAnim == null) {
			mFrame = mIndex = 0;
			complete = true;
			updateBuffer();
			return null;
		}
		mIndex = 0;
		mTimer = 0;
		mFrame = mAnim.mFrames[0];
		complete = false;
		updateBuffer();
		return mAnim;
	}
	
	/**
	 * Gets the frame index of the first column and row of the source image.
	 * @param	column		Frame column.
	 * @param	row			Frame row.
	 * @return	Frame index.
	 */
	public int getFrame() {
		return getFrame(0, 0);
	}
	/**
	 * Gets the frame index based on the column and first row of the source image.
	 * @param	column		Frame column.
	 * @param	row			Frame row.
	 * @return	Frame index.
	 */
	public int getFrame(int column) {
		return getFrame(column, 0);
	}
	/**
	 * Gets the frame index based on the column and row of the source image.
	 * @param	column		Frame column.
	 * @param	row			Frame row.
	 * @return	Frame index.
	 */
	public int getFrame(int column, int row) {
		return (row % mRows) * mColumns + (column % mColumns);
	}
	

	/**
	 * Sets the current display frame based on the column and first row of the source image.
	 * When you set the frame, any animations playing will be stopped to force the frame.
	 * @param	column		Frame column.
	 */
	public void setFrame(int column) {
		setFrame(column, 0);
	}
	
	/**
	 * Sets the current display frame based on the column and row of the source image.
	 * When you set the frame, any animations playing will be stopped to force the frame.
	 * @param	column		Frame column.
	 * @param	row			Frame row.
	 */
	public void setFrame(int column, int row) {
		mAnim = null;
		int frame = (row % mRows) * mColumns + (column % mColumns);
		if (mFrame == frame) 
			return;
		mFrame = frame;
		Log.d(TAG, "Setting frame to "+ frame);

		updateBuffer();
	}
	
	/**
	 * Assigns the Spritemap to a random frame.
	 */
	public void randFrame() {
		setFrameIndex(FP.rand(mFrameCount));
	}
	
	/**
	 * Sets the frame to the frame index of an animation.
	 * @param	name	Animation to draw the frame frame.
	 * @param	index	Index of the frame of the animation to set to.
	 */
	public void setAnimFrame(String name, int index) {
		Anim a = mAnims.get(name);
		if (a != null) {
			int frames[] = a.mFrames;
			index %= frames.length;
			if (index < 0)
				index += frames.length;
			setFrameIndex(frames[index]);
		}
	}
	
	/**
	 * Gets the current frame index.
	 */
	public int getFrameIndex() { return mFrame; }
	
	/**
	 * Sets the current frame index. When you set this, any
	 * animations playing will be stopped to force the frame.
	 */
	public void setFrameIndex(int value) {
		mAnim = null;
		value %= mFrameCount;
		if (value < 0)
			value = mFrameCount + value;
		if (mFrame == value) 
			return;
		mFrame = value;
		updateBuffer();
	}
	
	/**
	 * Current index of the playing animation.
	 */
	public int getAnimIndex() { return mAnim != null ? mIndex : 0; }
	/**
	 * Current index of the playing animation.
	 */
	public void setAnimIndex(int value) {
		if (mAnim == null)
			return;
		value %= mAnim.mFrameCount;
		if (mIndex == value)
			return;
		mIndex = value;
		mFrame = mAnim.mFrames[mIndex];
		updateBuffer();
	}
	
	/**
	 * The amount of frames in the Spritemap.
	 */
	public int getFrameCount() { return mFrameCount; }

	/**
	 * Columns in the Spritemap.
	 */
	public int getColumns() { return mColumns; }

	/**
	 * Rows in the Spritemap.
	 */
	public int getRows() { return mRows; }
	
	/**
	 * The currently playing animation.
	 */
	public String getCurrentAnim() { return mAnim != null ? mAnim.mName : ""; }
}