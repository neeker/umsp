package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SmgpTlvDataPacket extends SmgpDataPacket {

	private static final long serialVersionUID = 2652914114267757671L;
	
	private Map<Short, SmgpTlvData> tlvDatas;

	public SmgpTlvDataPacket(int command) {
		super(command);
	}

	protected Map<Short, SmgpTlvData> resolveTlvDatas() {
		if (tlvDatas == null) {
			tlvDatas = new LinkedHashMap<Short, SmgpTlvDataPacket.SmgpTlvData>();
		}
		return tlvDatas;
	}
	
	public void setDynamicProperty(short tag, byte[] value) {
		resolveTlvDatas().put(tag, new SmgpTlvData(tag, value));
	}
	
	public byte[] removeDynamicProperty(short tag) {
		if (tlvDatas == null) return null;
		SmgpTlvData tlv = tlvDatas.remove(tag);
		return tlv == null ? null : tlv.value;
	}
	
	public boolean hasDynamicProperty(short tag) {
		return tlvDatas == null ? false : tlvDatas.containsKey(tag);
	}
	
	public byte[] getDynamicProperty(short tag) {
		return hasDynamicProperty(tag) ? tlvDatas.get(tag).value : null;
	}

	protected void writeTlvDatas(DataOutput out) throws IOException {
		resolveTlvDatas();
		for (SmgpTlvData tlv : tlvDatas.values()) {
			if (tlv != null) {
				out.writeShort(tlv.tag);
				short len = tlv.getShortLen();
				out.writeShort(len);
				out.write(tlv.value, 0, len);
			}
		}
	}

	protected void readTlvDatas(DataInput in) throws IOException {
		tlvDatas = null;
		Map<Short, SmgpTlvDataPacket.SmgpTlvData> temp_values = new LinkedHashMap<Short, SmgpTlvDataPacket.SmgpTlvData>();
		while (true) {
			SmgpTlvData tlv = new SmgpTlvData();
			try {
				tlv.tag = in.readShort();
				short len = in.readShort();
				tlv.value = new byte[len];
				in.readFully(tlv.value, 0, len);
				temp_values.put(tlv.tag, tlv);
			} catch (IOException e) {
				break;
			}
		}
		tlvDatas = temp_values;
	}

	protected int getTlvDataBufferLength() {
		int result = 0;
		if (tlvDatas != null) {
			for (SmgpTlvData tlv : tlvDatas.values()) {
				if (tlv != null) {
					result = result + 4 + tlv.getShortLen();
				}
			}
		}
		return result;
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + getTlvDataBufferLength();
	}
	
	@Override
	public SmgpDataPacket clone() {
		SmgpTlvDataPacket cloned = (SmgpTlvDataPacket) super.clone();
		return cloned;
	}

	public static class SmgpTlvData implements Serializable, Cloneable {
		
		private static final long serialVersionUID = 90L;
		
		public short tag;

		public byte[] value;

		public SmgpTlvData() {
		}

		public SmgpTlvData(short tag) {
			this.tag = tag;
		}

		public short getShortLen() {
			return (short) (value == null ? 0 : value.length);
		}

		public SmgpTlvData(short tag, byte[] value) {
			this.tag = tag;
			this.value = value;
		}
		
		@Override
		public SmgpTlvData clone() {
			try {
				SmgpTlvData data = (SmgpTlvData) super.clone();
				data.tag = tag;
				data.value = value.clone();
				return data;
			} catch (CloneNotSupportedException e) {
				throw new IllegalStateException();
			}
		}

	}
}
