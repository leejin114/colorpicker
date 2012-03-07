/**
 *  Color Picker by Juan Martín
 *  Copyright (C) 2010 nauj27.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.nauj27.android.colorpicker;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * This activity make focus and take a photo.
 * @author nauj27
 * Extends Activity
 */
public class TakePhotoActivity extends Activity {
	
	private static final int MENU_TAKE_PHOTO_ITEM = 0;
	private static final int MENU_ABOUT_ITEM = 1;
	private static final int MENU_EXIT_ITEM = 10; // The last one.
	
	private static final int DIALOG_ABOUT_ID = 0;
	
	private Camera camera = null;
	private int displayWidth = 0;
	private int displayHeight = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		// From http://www.designerandroid.com/?p=73
    	// This is the only way camera preview work on all android devices
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		// No title, no name: Full screen
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.take_photo_layout);
		
		ImageButton imageCameraButton = (ImageButton)findViewById(R.id.ImageCameraButton);
		imageCameraButton.setOnClickListener(onClickListener);
		
		SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceViewCamera);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(surfaceCallback);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
	}
	
	/**
	 * Create the on click listener for the button.
	 */
	private View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			try {
				camera.autoFocus(autoFocusCallback);
			} catch (Exception e) {
				//Log.e(TAG, "Autofocus exception.");
			}
		}
	};
	
	/**
	 * Autofocus callback.
	 */
	private AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean arg0, Camera camera) {
			try {
				camera.takePicture(null, null, jpegCallback);
			} catch (RuntimeException runtimeException) {
				Toast toast = Toast.makeText(getBaseContext(), 
						R.string.camera_error, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	};
	
	/**
	 * JPEG data is available after take picture.
	 * Put data into extras of Intent and launch new intent for ColorPicker.
	 */
	private PictureCallback jpegCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] jpegPicture, Camera camera) {
			final String JPEG_PICTURE = "JPEG_PICTURE";
			
			Intent colorPickerIntent = new Intent(getApplicationContext(), ColorPickerActivity.class);
			colorPickerIntent.putExtra(JPEG_PICTURE, jpegPicture);
			startActivity(colorPickerIntent);
		}
	};
	
	/**
	 * Create the surface callback for 
	 */
	private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}

		@Override
		public void surfaceChanged(
				SurfaceHolder surfaceHolder, int format, int width, int height) {
			
			if (camera != null) {				
				Size size = null;
				Parameters parameters = camera.getParameters();
			
				// Get and set a supported picture size if any
				List<Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
				size = Utils.getBestSize(camera, supportedPictureSizes, false,
						displayWidth, displayHeight);

				try {
					parameters.setPictureSize(size.width, size.height);
				} catch (NullPointerException e) {
					// Notify the user and exit the application
				}
				
				// Get and set a supported preview size if any
				List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
				size = Utils.getBestSize(camera, supportedPreviewSizes, true,
						displayWidth, displayHeight);
				
				try {
					parameters.setPreviewSize(size.width, size.height);
				} catch (NullPointerException e) {
					// Notify the user and exit the application
				}
				
				// Retrieve and set the supported picture format
				List<Integer> supportedPictureFormats = parameters.getSupportedPictureFormats();
				if (supportedPictureFormats.contains(PixelFormat.JPEG)) {
					parameters.setPictureFormat(PixelFormat.JPEG);
				}
				
				camera.setParameters(parameters);
				camera.startPreview();
			} else {
				// Notify the user and exit the application
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder surfaceHolder) {
			try {
				camera = Camera.open();
				
			} catch (RuntimeException rtException) {
				if (camera != null) {
					camera.release();
					camera = null;
				}
			}
			
			
			try {
				camera.setPreviewDisplay(surfaceHolder);
				
			} catch (NullPointerException npException) {
				// If we can't open camera show error and exist app
				CharSequence charSequence = getString(R.string.camera_error);
		    	int duration = Toast.LENGTH_LONG;
		    	Toast toast = Toast.makeText(
		    			getApplicationContext(), 
		    			charSequence, 
		    			duration);
		    	toast.show();
		    	finish();
		    	
			} catch (IOException ioException) {
				if (camera != null) {
					camera.release();
					camera = null;
				}
			}
		}
	};
	
    @Override
    /**
     * Add items to the menu for this activity.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	final int groupId = 0;
    	int menuItemId;
    	int menuItemOrder;
    	int menuItemText;
    	MenuItem menuItem;

    	// Button to take picture
    	menuItemId = MENU_TAKE_PHOTO_ITEM;
    	menuItemOrder = MENU_TAKE_PHOTO_ITEM;
    	menuItemText = R.string.menu_take_photo_item;
    	menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
    	menuItem.setIcon(android.R.drawable.ic_menu_camera);
    	
    	// Button to take picture
    	menuItemId = MENU_ABOUT_ITEM;
    	menuItemOrder = MENU_ABOUT_ITEM;
    	menuItemText = R.string.menu_take_photo_about;
    	menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
    	menuItem.setIcon(android.R.drawable.ic_menu_info_details);
    	
    	// Button to exit the application
    	menuItemId = MENU_EXIT_ITEM;
    	menuItemOrder = MENU_EXIT_ITEM;
    	menuItemText = R.string.menu_take_photo_exit;
    	menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
    	menuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	
    	return true;
    }
    
    @Override
    /**
     * What to do when a option menu is selected.
     * @param menuItem Item selected from menu.
     */
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	super.onOptionsItemSelected(menuItem);
    	
    	// Search for menu item.
    	switch(menuItem.getItemId()) {
	    	case(MENU_TAKE_PHOTO_ITEM):
	    		camera.autoFocus(autoFocusCallback);
	    		break;
	    	case(MENU_ABOUT_ITEM):
	    		showDialog(DIALOG_ABOUT_ID);
	    		break;
	    	case(MENU_EXIT_ITEM):
	    		finish();
	    		break;
	    	default:
	    		return false;
    	}
    	return true;
    }
    
    @Override
    /**
     * Overrides the "oncreatedialog" method for the activity.
     * Create and return the about dialog.
     */
    protected Dialog onCreateDialog(int id) {
    	AlertDialog alertDialog;
    	
        switch(id) {
        case DIALOG_ABOUT_ID:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle(R.string.app_name)
        		.setMessage(R.string.about_text);
        	alertDialog = builder.create();
            break;
        default:
            alertDialog = null;
        }
        return alertDialog;
    }
    
    @Override 
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
}
