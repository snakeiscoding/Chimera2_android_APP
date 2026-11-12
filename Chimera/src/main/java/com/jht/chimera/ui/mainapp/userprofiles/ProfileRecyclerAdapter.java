package com.jht.chimera.ui.mainapp.userprofiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.service.dapi.UserProfileManager;


public class ProfileRecyclerAdapter extends RecyclerView.Adapter<ProfileRecyclerAdapter.MyHolder> {
    protected UserProfileManager.ProfileDataStore mData;
    protected ProfileEvents eventHandler;

    public interface ProfileEvents {
        void onProfileSelected(UserProfileManager.ProfileUser user);
        void onEditProfile(UserProfileManager.ProfileUser user);
        void OnNewUser();
    }

    public ProfileRecyclerAdapter(UserProfileManager.ProfileDataStore profileData, ProfileEvents events) {
        super();
        mData = profileData;
        eventHandler = events;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.profile_holder, viewGroup, false);
        MyHolder mh = new MyHolder(itemView);
        mh.buttonEdit.setOnClickListener((View v) -> {
            if (eventHandler!=null) eventHandler.onEditProfile(mh.user);
        });
        mh.thumbnail.setOnClickListener((View v) -> {
            if (eventHandler!=null) {
                if (mh.user == null) eventHandler.OnNewUser();
                else eventHandler.onProfileSelected(mh.user);
            }
        });

        return mh;
    }

    @Override
    public void onBindViewHolder(MyHolder myHolder, int i) {
        if (i < mData.users.size()) {
            UserProfileManager.ProfileUser item = mData.users.get(i);
            myHolder.setUser(item, i);
        }
        else if (i == mData.users.size()) myHolder.setGuest();
        else myHolder.setNewUser();
    }

    @Override
    public int getItemCount() { return Math.min(mData.users.size() + 2, UserProfileManager.MAX_USER_LIMIT + 1); }


    public static class MyHolder extends RecyclerView.ViewHolder {
        protected TextView name;
        protected TextView initials;
        protected Button buttonEdit;
        protected ImageView image;
        protected FrameLayout thumbnail;
        protected final int colors[] = {R.color.profile_user1, R.color.profile_user2, R.color.profile_user3, R.color.profile_user4};
        protected UserProfileManager.ProfileUser user;

        private MyHolder(View v) {
            super(v);
            this.name = (TextView) v.findViewById(R.id.name);
            this.initials = (TextView) v.findViewById(R.id.initials);
            this.buttonEdit = v.findViewById(R.id.profile_edit);
            this.image = v.findViewById(R.id.img_profile);
            this.thumbnail = v.findViewById(R.id.thumbnail_profile);
        }

        public void setUser(UserProfileManager.ProfileUser userProfile, int index) {
            user = userProfile;
            this.name.setText(user.getFullName());
            this.buttonEdit.setVisibility(View.VISIBLE);
            this.initials.setText(user.getInitials());
            this.image.setVisibility(View.GONE);
            this.thumbnail.setBackgroundResource(colors[index % 4]);
        }

        public void setGuest() {
            user = UserProfileManager.guestProfile;
            this.name.setText(R.string.guest_tc);
            this.buttonEdit.setVisibility(View.INVISIBLE);
            // show icon instead of initials
            this.initials.setText("");
            //this.image.setContentDescription(R.string.guest_tc);
            this.image.setImageResource(R.drawable.ic_login); // TODO - is this the correct icon? looks slightly different from example?
            this.image.setVisibility(View.VISIBLE);
            this.thumbnail.setBackgroundResource(R.color.profile_guest);
        }

        public void setNewUser() {
            user = null;
            this.name.setText(R.string.new_user_tc);
            this.buttonEdit.setVisibility(View.INVISIBLE);
            this.initials.setText("");
            this.image.setImageResource(R.drawable.ic_plus);
            this.image.setVisibility(View.VISIBLE);
            this.thumbnail.setBackgroundResource(R.color.profile_new);
        }
    }
}
