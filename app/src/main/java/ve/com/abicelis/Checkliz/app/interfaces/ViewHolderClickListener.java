package ve.com.abicelis.Checkliz.app.interfaces;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;


public interface ViewHolderClickListener {
    public void onItemClicked(int position, @Nullable Intent optionalIntent, @Nullable Bundle optionalBundle);
    public boolean onItemLongClicked(int position);
}
