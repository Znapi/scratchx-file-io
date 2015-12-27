package znapi.fileiohelper;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class TableModel extends AbstractTableModel {

	private final static String[] cNames = {"File", "Allow Reading", "Allow Writing"};
	public ArrayList<Object[]> data;

	public TableModel() {
		data = new ArrayList<Object[]>();
	}

	public int getRowCount() {
		return data.size();
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int c) {
		return cNames[c];
	}

	public Object getValueAt(int r, int c) {
		return data.get(r)[c];
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Class getColumnClass(int c) {
		if(c == 0) return String.class;
		else return Boolean.class;
	}

	public boolean isCellEditable(int row, int col) {
		if(col == 0) return false;
		else return true;
	}

	public void setValueAt(Object value, int r, int c) {
		data.get(r)[c] = value;
		fireTableCellUpdated(r, c);
	}

	public void addRow(String dir, boolean allowRead, boolean allowWrite) {
		data.add(new Object[]{dir, allowRead, allowWrite});
		//this.fireTableDataChanged();
	}

}
