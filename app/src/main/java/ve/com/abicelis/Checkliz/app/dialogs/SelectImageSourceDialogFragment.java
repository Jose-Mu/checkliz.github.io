package ve.com.abicelis.Checkliz.app.dialogs;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import java.util.List;

import ve.com.abicelis.Checkliz.R;
import ve.com.abicelis.Checkliz.enums.ImageSourceType;

public class SelectImageSourceDialogFragment extends DialogFragment implements View.OnClickListener {

    //DATA
    private SelectImageSourceSelectedListener mListener;
    private Drawable mCameraDrawable;
    private Drawable mGalleryDrawable;

    //UI
    private ImageButton mCamera;
    private ImageButton mGallery;
    private Button mCancel;


    public SelectImageSourceDialogFragment() {
    }

    public static SelectImageSourceDialogFragment newInstance() {
        SelectImageSourceDialogFragment frag = new SelectImageSourceDialogFragment();
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        PackageManager manager = getActivity().getPackageManager();


        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);

        List<ResolveInfo> cameraAppList = manager.queryIntentActivities(cameraIntent, 0);
        List<ResolveInfo> galleryAppList = manager.queryIntentActivities(galleryIntent, 0);

        if (cameraAppList.size() > 0)
            mCameraDrawable = cameraAppList.get(0).loadIcon(manager);

        if(galleryAppList.size() > 0)
            mGalleryDrawable = galleryAppList.get(0).loadIcon(manager);

//    @Override
//    public void onResume() {
//        super.onResume();
//
//        int height = getResources().getDimensionPixelSize(R.dimen.dialog_select_image_source_height);
//        int width = getResources().getDimensionPixelSize(R.dimen.dialog_select_image_source_width);
//
//        getDialog().getWindow().setLayout(width, height);
//    }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView =  inflater.inflate(R.layout.dialog_image_source_select, container);

        mCamera = (ImageButton) dialogView.findViewById(R.id.dialog_image_source_select_camera);
        mGallery = (ImageButton) dialogView.findViewById(R.id.dialog_image_source_select_gallery);
        mCancel = (Button) dialogView.findViewById(R.id.dialog_image_source_select_cancel);

        if(mCameraDrawable != null)
            mCamera.setBackground(mCameraDrawable);

        if(mGalleryDrawable != null)
            mGallery.setBackground(mGalleryDrawable);

        mCamera.setOnClickListener(this);
        mGallery.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        return dialogView;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(mListener != null)
            mListener.onSourceSelected(ImageSourceType.NONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.dialog_image_source_select_camera:
                if(mListener != null)
                    mListener.onSourceSelected(ImageSourceType.CAMERA);
                dismiss();
                break;

            case R.id.dialog_image_source_select_gallery:
                if(mListener != null)
                    mListener.onSourceSelected(ImageSourceType.GALLERY);
                dismiss();
                break;

            case R.id.dialog_image_source_select_cancel:
                if(mListener != null)
                    mListener.onSourceSelected(ImageSourceType.NONE);
                dismiss();
                break;
        }
    }


    public void setListener(SelectImageSourceSelectedListener listener) {
        mListener = listener;
    }


    public interface SelectImageSourceSelectedListener {
        void onSourceSelected(ImageSourceType imageSourceType);
    }
}
