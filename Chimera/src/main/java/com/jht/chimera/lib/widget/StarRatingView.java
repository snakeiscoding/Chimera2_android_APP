package com.jht.chimera.lib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
//import com.jhtp.dapiservice.an.events.StarRatingEvent;

public class StarRatingView extends ConstraintLayout {
    private static final String TAG = "StarRatingView";
    private static final int STAR1VALUE = 1;
    private static final int STAR2VALUE = 2;
    private static final int STAR3VALUE = 3;
    private static final int STAR4VALUE = 4;
    private static final int STAR5VALUE = 5;

    private IStarRated mCallback;
    private int questionId;
    private int selectedStarValue;

    private ImageView mStar1;
    private ImageView mStar2;
    private ImageView mStar3;
    private ImageView mStar4;
    private ImageView mStar5;

    public StarRatingView(Context context)
    {
        super(context);
        initializeView(context);
    }

    public StarRatingView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initializeView(context);
    }

    public StarRatingView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initializeView(context);
    }

    private void initializeView(Context context)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.star_rating_layout, this);

        mStar1 = findViewById(R.id.star1);
        mStar2 = findViewById(R.id.star2);
        mStar3 = findViewById(R.id.star3);
        mStar4 = findViewById(R.id.star4);
        mStar5 = findViewById(R.id.star5);

        mStar1.setOnClickListener(v -> setStarImages(STAR1VALUE));
        mStar2.setOnClickListener(v -> setStarImages(STAR2VALUE));
        mStar3.setOnClickListener(v -> setStarImages(STAR3VALUE));
        mStar4.setOnClickListener(v -> setStarImages(STAR4VALUE));
        mStar5.setOnClickListener(v -> setStarImages(STAR5VALUE));
    }

    public void sendAnalytics() {
        /*if (selectedStarValue > 0) {
            DataMatrix.putParcelable(DAPIKeys.EVENT_DAPI_STAR_RATING_SELECTED_PARCELABLE, new StarRatingEvent(questionId, selectedStarValue));
        }*/
    }

    public void setQuestion (int questionId, String questionText){
        TextView question = findViewById(R.id.question);
        question.setText(questionText);
        this.questionId = questionId;
    }

    public interface IStarRated {
        void onStarRated(int selectedStarValue);
    }

    public void setStarSelectedListener(IStarRated callback){
        mCallback = callback;
    }

    private void setStarImages(int selectedStarValue){
        this.selectedStarValue = selectedStarValue;
        mStar1.setImageResource(selectedStarValue >= STAR1VALUE ? R.drawable.ic_star : R.drawable.ic_star_empty);
        mStar2.setImageResource(selectedStarValue >= STAR2VALUE ? R.drawable.ic_star : R.drawable.ic_star_empty);
        mStar3.setImageResource(selectedStarValue >= STAR3VALUE ? R.drawable.ic_star : R.drawable.ic_star_empty);
        mStar4.setImageResource(selectedStarValue >= STAR4VALUE ? R.drawable.ic_star : R.drawable.ic_star_empty);
        mStar5.setImageResource(selectedStarValue >= STAR5VALUE ? R.drawable.ic_star : R.drawable.ic_star_empty);

        int selectedColor = getContext().getResources().getColor(R.color.matrixYellow);
        int unselectedColor = getContext().getResources().getColor(R.color.white);
        mStar1.setColorFilter(selectedStarValue >= STAR1VALUE ? selectedColor : unselectedColor);
        mStar2.setColorFilter(selectedStarValue >= STAR2VALUE ? selectedColor : unselectedColor);
        mStar3.setColorFilter(selectedStarValue >= STAR3VALUE ? selectedColor : unselectedColor);
        mStar4.setColorFilter(selectedStarValue >= STAR4VALUE ? selectedColor : unselectedColor);
        mStar5.setColorFilter(selectedStarValue >= STAR5VALUE ? selectedColor : unselectedColor);

        if (mCallback != null) {
            mCallback.onStarRated(selectedStarValue);
        }
    }
}
