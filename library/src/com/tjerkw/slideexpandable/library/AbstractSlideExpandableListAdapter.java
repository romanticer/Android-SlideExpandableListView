package com.tjerkw.slideexpandable.library;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wraps a ListAdapter to give it expandable list view functionality.
 * The main thing it does is add a listener to the getToggleButton
 * which expands the getExpandableView for each list item.
 *
 * @author tjerk
 * @date 6/9/12 4:41 PM
 */
public abstract class AbstractSlideExpandableListAdapter extends WrapperListAdapterImpl {
	private View lastOpen = null;
	private int lastOpenPosition = -1;
	private Set<Integer> openItems = new HashSet<Integer>(10);
	private Map<Integer, Integer> viewHeights = new HashMap<Integer, Integer>(10);

	public AbstractSlideExpandableListAdapter(ListAdapter wrapped) {
		super(wrapped);
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		view = wrapped.getView(position, view, viewGroup);
		enableFor(view, position);
		return view;
	}

	/**
	 * This method is used to get the Button view that should
	 * expand or collapse the Expandable View.
	 * <br/>
	 * Normally it will be implemented as:
	 * <pre>
	 * return (Button)parent.findViewById(R.id.expand_toggle_button)
	 * </pre>
	 *
	 * A listener will be attached to the button which will
	 * either expand or collapse the expandable view
	 *
	 * @see getExpandableView
	 * @param parent the list view item
	 * @return a child of parent which is a button
	 */
	public abstract Button getExpandToggleButton(View parent);

	/**
	 * This method is used to get the view that will be hidden
	 * initially and expands or collapse when the ExpandToggleButton
	 * is pressed @see getExpandToggleButton
	 * <br/>
	 * Normally it will be implemented as:
	 * <pre>
	 * return parent.findViewById(R.id.expandable)
	 * </pre>
	 *
	 * @see getExpandToggleButton
	 * @param parent the list view item
	 * @return a child of parent which is a view (or often ViewGroup)
	 *  that can be collapsed and expanded
	 */
	public abstract View getExpandableView(View parent);

	/**
	 * Gets the duration of the collapse animation in ms.
	 * Default is 330ms. Override this method to change the default.
	 *
	 * @return the duration of the anim in ms
	 */
	protected int getAnimationDuration() {
		return 330;
	}

	public void enableFor(View parent, int position) {
		Button more = getExpandToggleButton(parent);
		View itemToolbar = getExpandableView(parent);
		enableFor(more, itemToolbar, position);
	}


	private void enableFor(View button, final View target, final int position) {
		if(target == lastOpen && position!=lastOpenPosition) {
			// lastOpen is recycled, so its reference is false
			lastOpen = null;
		}
		if(position == lastOpenPosition) {
			lastOpen = target;
		}
		if(viewHeights.get(position)==null) {

			// just before drawing we know that the measurement
			// was done correctly, so at this point we remember the height
			// of the target view, so we know it when animating
			target.getViewTreeObserver().addOnPreDrawListener(
				new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						target.getViewTreeObserver().removeOnPreDrawListener(this);
						Log.d("AbstractSlideExpandable", "gotHeight: " + target.getHeight());
						viewHeights.put(position, target.getHeight());
						updateExpandable(target, position);
						return false;
					}
				}
			);
		} else {
			updateExpandable(target, position);
		}

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				view.setAnimation(null);
				int type = target.getVisibility() == View.VISIBLE ? ExpandCollapseAnimation.COLLAPSE : ExpandCollapseAnimation.EXPAND;
				Animation anim = new ExpandCollapseAnimation(
					target,
					getAnimationDuration(),
					type
				);
				if(type == ExpandCollapseAnimation.EXPAND) {
					openItems.add(position);
				} else {
					openItems.remove(position);
				}
				if(type == ExpandCollapseAnimation.EXPAND) {
					if(lastOpenPosition != -1 && lastOpenPosition != position) {
						if(lastOpen!=null) {
							lastOpen.startAnimation(
								new ExpandCollapseAnimation(
									lastOpen,
									getAnimationDuration(),
									ExpandCollapseAnimation.COLLAPSE
								)
							);
						}
						openItems.remove(lastOpenPosition);
					}
					lastOpen = target;
					lastOpenPosition = position;
				} else if(lastOpenPosition == position) {
					lastOpenPosition = -1;
				}
				view.startAnimation(anim);
			}
		});
	}

	private void updateExpandable(View target, int position) {

		final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)target.getLayoutParams();
		if(openItems.contains(position)) {
			target.setVisibility(View.VISIBLE);
			params.bottomMargin = 0;
		} else {
			target.setVisibility(View.GONE);
			params.bottomMargin = 0-viewHeights.get(position);
		}
	}

}
