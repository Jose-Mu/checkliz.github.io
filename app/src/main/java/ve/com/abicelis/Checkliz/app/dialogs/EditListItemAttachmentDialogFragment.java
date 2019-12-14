package ve.com.abicelis.Checkliz.app.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import ve.com.abicelis.Checkliz.R;

public class EditListItemAttachmentDialogFragment extends DialogFragment implements View.OnClickListener {

    //DATA
    private boolean isANewItem;

    //UI
    private EditListItemAttachmentDialogDismissListener mListener;
    private EditText mText;
    private Button mCancel;
    private Button mOk;

    public EditListItemAttachmentDialogFragment() {

    }

    public static EditListItemAttachmentDialogFragment newInstance(boolean isANewItem, String text) {
        EditListItemAttachmentDialogFragment frag = new EditListItemAttachmentDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("isANewItem", isANewItem);
        args.putString("text", text);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        final String text = getArguments().getString("text");
        isANewItem = getArguments().getBoolean("isANewItem", false);

        View dialogView =  inflater.inflate(R.layout.dialog_edit_list_item_attachment_text, container);

        mText = (EditText) dialogView.findViewById(R.id.dialog_edit_list_item_attachment_text);
        mText.setText(text);
        mText.setSelection(mText.getText().length());

        mOk = (Button) dialogView.findViewById(R.id.dialog_edit_list_item_attachment_ok);
        mOk.setOnClickListener(this);
        mCancel = (Button) dialogView.findViewById(R.id.dialog_edit_list_item_attachment_cancel);
        mCancel.setOnClickListener(this);

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialogView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.dialog_edit_list_item_attachment_cancel:
                //sembunyikan keyboard
                ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mText.getWindowToken(), 0);

                dismiss();
                break;

            case R.id.dialog_edit_list_item_attachment_ok:
                if(!mText.getText().toString().trim().isEmpty()) {

                    mListener.onFinishEditListItemAttachmentDialog(mText.getText().toString().trim(), isANewItem);

                    //sembunyikan keyboard
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mText.getWindowToken(), 0);

                    dismiss();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.dialog_edit_list_item_error_empty_text), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void setListener(EditListItemAttachmentDialogDismissListener listener) {
        mListener = listener;
    }


    public interface EditListItemAttachmentDialogDismissListener {
        void onFinishEditListItemAttachmentDialog(String text, boolean isANewItem);
    }
}
