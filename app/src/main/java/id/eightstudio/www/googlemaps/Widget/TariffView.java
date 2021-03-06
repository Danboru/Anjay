package id.eightstudio.www.googlemaps.Widget;


import android.widget.FrameLayout;
import android.util.AttributeSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import id.eightstudio.www.googlemaps.R;
import android.widget.TextView;
import android.graphics.Paint;
import com.google.android.gms.maps.*;
import android.widget.RadioButton;
import android.widget.CompoundButton;

public class TariffView extends FrameLayout implements CompoundButton.OnCheckedChangeListener
{

	TextView jarak, oldprice,lowprice,normprice,reload;
	private View view, myLoc, jarakBar, rootv;
	public static int myHeight=0;
	public static int[] jarakBarHeight=new int[2];
	private RadioButton rb1, rb2;
	private GoogleMap.OnMyLocationButtonClickListener callmyloc;

	public TariffView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public TariffView(Context context, AttributeSet attrs) {
        super(context, attrs);
		initView();
    }

    public TariffView(Context context) {
        super(context);
        initView();
    }

	private void initView() {

        view = LayoutInflater.from(getContext()).inflate(R.layout.tariff_view, null);

	    jarak = view.findViewById(R.id.tariffviewJarak);
		oldprice = view.findViewById(R.id.tariffviewOldPrice);
		normprice = view.findViewById(R.id.tariffviewNormalPrice);
		lowprice = view.findViewById(R.id.tariffviewLowPrice);
		reload = view.findViewById(R.id.tariffviewReload);
		rb1 = view.findViewById(R.id.sbRadioButton1);
		rb2 = view.findViewById(R.id.sbRadioButton2);
		jarakBar = view.findViewById(R.id.tariffviewLinearLayout1);
		myLoc = view.findViewById(R.id.tariffviewMyLoc);
		rootv = view.findViewById(R.id.tariffviewLinearLayout2);

		oldprice.setPaintFlags(oldprice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

		myLoc.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View p1) {
					if(callmyloc!=null) callmyloc.onMyLocationButtonClick();
				}
			});

		post(new Runnable(){
				@Override
				public void run() {
					myHeight=getHeight();
				}
			});

		rb1.setOnCheckedChangeListener(this);
		rb2.setOnCheckedChangeListener(this);
		rb2.setChecked(true);
		addView(view);
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2) {
		if(rb1==p1&&p2) {
			rb2.setChecked(false);
		} else if(rb2 == p1 && p2) {
			rb1.setChecked(false);
		}
	}

	public void setJarak(String s){
		jarak.setText(s);
	}

	public void setOnMyLocationButtonClickListener(GoogleMap.OnMyLocationButtonClickListener x){
		callmyloc = x;
	}

	public void hide(final boolean animate){
		rootv.post(new Runnable(){
				@Override
				public void run() {
					animate().setStartDelay(0);
					animate().setDuration(animate ? 500 : 0);
					animate().translationY(rootv.getHeight());
					animate().start();
				}
			});
	}

	public void show(){
		animate().setStartDelay(1000);
		animate().setDuration(1000);
		animate().translationY(0);
		animate().start();
	}

	//TODO : Perhitungan tarrif berdasarkan jarak
	public void setTarifByJarak(double d){

		long tarif = 0;
		long tarifdisc = 0;

        // Jarak 1 sampai 3 KM Tarif = 6000
		if( d <= 3 ){
			tarif = 6000;
			tarifdisc = tarif - 1000;
		} else {
			tarif = Math.round(((d-3)*2000) + 6000);
			tarifdisc = tarif - (tarif > 20000 ? 10000:1000);
		}

		long roundedTarif = ((tarif + 99) / 100 ) * 100;
		long roundedTarifdisc = ((tarifdisc + 99) / 100 ) * 100;

		normprice.setText(priceFormater(roundedTarif, "Rp"));
		oldprice.setText(priceFormater(roundedTarif, "Rp"));
		lowprice.setText(priceFormater(roundedTarifdisc, "Rp"));
	}

	public String priceFormater(long s, String currency){
		return (currency+s).replaceAll("(\\d)(?=(\\d{3})+(?!\\d))", "$1.");
	}

}
