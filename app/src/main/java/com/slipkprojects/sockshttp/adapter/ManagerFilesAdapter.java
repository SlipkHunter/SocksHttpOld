package com.slipkprojects.sockshttp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.util.List;
import com.slipkprojects.sockshttp.R;
import android.widget.LinearLayout;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;

public class ManagerFilesAdapter
	extends RecyclerView.Adapter<ManagerFilesAdapter.ItemHolder>
{
	
	private List<ManagerItem> items;
	private LayoutInflater layoutInflater;
	private OnItemClickListener clickListener;

	
	public interface OnItemClickListener {
		void onItemClick(View view, int position);
		void onItemLongClick(View view, int position);
	}
	
	public static class ManagerItem implements Comparable<ManagerItem> {
		private String dirName;
		private String dirPath;
		private String dirNameSecond;

		public ManagerItem(String dirName, String dirPath, String dirNameSecond) {
			this.dirName = dirName;
			this.dirPath = dirPath;
			this.dirNameSecond = dirNameSecond;
		}

		public void setDirName(String dirName) {
			this.dirName = dirName;
		}

		public String getDirName() {
			return dirName;
		}

		public void setDirPath(String dirPath) {
			this.dirPath = dirPath;
		}

		public String getDirPath() {
			return dirPath;
		}
		
		public void setNameSecond(String str) {
			this.dirNameSecond = str;
		}
		
		public String getNameSecond() {
			return dirNameSecond;
		}

		@Override
		public int compareTo(ManagerItem managerDir) {
			return dirName.compareToIgnoreCase(managerDir.getDirName());
		}
	}
	
	public class ItemHolder extends RecyclerView.ViewHolder {
		TextView tvItemName;
		ImageView ivImage;
		LinearLayout tvItemLayout;
		TextView tvItemNameSecond;

		public ItemHolder(View view) {
			super(view);
			tvItemLayout = view.findViewById(R.id.ivManagerAdapter_ItemLayout);
			tvItemName = view.findViewById(R.id.tvManagerAdapter_FolderName);
			ivImage = view.findViewById(R.id.ivManagerAdapter_ImageIcon);
			tvItemNameSecond = view.findViewById(R.id.tvManagerAdapter_FolderNameDate);
		}
	}
	
	
	public ManagerFilesAdapter(Context context, List<ManagerItem> items) {
		this.layoutInflater = LayoutInflater.from(context);
		this.items = items;
	}

	@Override
	public ManagerFilesAdapter.ItemHolder onCreateViewHolder(ViewGroup parent, int position) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_manager, parent, false);
		ItemHolder item = new ItemHolder(view);
		return item;
	}

	@Override
	public void onBindViewHolder(final ManagerFilesAdapter.ItemHolder item,
			final int position) {
		ManagerItem manager = items.get(position);
		String dirName = manager.getDirName();
		item.tvItemName.setText(dirName);
		item.tvItemNameSecond.setText(manager.getNameSecond());
		
		if (new File(manager.getDirPath()).isFile()) {
			item.ivImage.setImageResource(R.drawable.file_icon);
		} else if (dirName.endsWith("...")) {
			item.ivImage.setImageResource(R.drawable.back_icon);
		} else {
			item.ivImage.setImageResource(R.drawable.folder_icon);
		}
		
		item.tvItemLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (clickListener != null) {
					clickListener.onItemClick(view, position);
				}
			}
		});

		item.tvItemLayout.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (clickListener != null) {
					clickListener.onItemLongClick(v, position);
				}
				return true;
			}
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void setOnItemClickListener(OnItemClickListener itemClickListener) {
		this.clickListener = itemClickListener;
	}
	
}
