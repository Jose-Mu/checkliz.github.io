package ve.com.abicelis.Checkliz.app.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import ve.com.abicelis.Checkliz.R;
import ve.com.abicelis.Checkliz.app.holders.PlaceViewHolder;
import ve.com.abicelis.Checkliz.model.Place;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceViewHolder> {

    //DATA
    private List<Place> mPlaces;
    private LayoutInflater mInflater;
    private Activity mActivity;

    public PlaceAdapter(Activity activity, List<Place> places) {
        mPlaces = places;
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);

    }

    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlaceViewHolder(mInflater.inflate(R.layout.list_item_place, parent, false));
    }


    @Override
    public void onBindViewHolder(PlaceViewHolder holder, int position) {
        holder.setData(this, mActivity, mPlaces.get(position), position);
        holder.setListeners();
    }

    @Override
    public int getItemCount() {
        return mPlaces.size();
    }
}
