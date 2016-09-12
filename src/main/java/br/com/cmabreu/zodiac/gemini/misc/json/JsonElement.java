package br.com.cmabreu.zodiac.gemini.misc.json;

public class JsonElement {
	private JsonPosition position;
	private String group;
	private String removed;
	private String selected;
	private String selectable;
	private String locked;
	private String grabbed;
	private String grabbable;
	private String classes;
	private JsonData data;
	
	public JsonData getData() {
		return data;
	}
	public void setData(JsonData data) {
		this.data = data;
	}
	public JsonPosition getPosition() {
		return position;
	}
	public void setPosition(JsonPosition position) {
		this.position = position;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getRemoved() {
		return removed;
	}
	public void setRemoved(String removed) {
		this.removed = removed;
	}
	public String getSelected() {
		return selected;
	}
	public void setSelected(String selected) {
		this.selected = selected;
	}
	public String getSelectable() {
		return selectable;
	}
	public void setSelectable(String selectable) {
		this.selectable = selectable;
	}
	public String getLocked() {
		return locked;
	}
	public void setLocked(String locked) {
		this.locked = locked;
	}
	public String getGrabbed() {
		return grabbed;
	}
	public void setGrabbed(String grabbed) {
		this.grabbed = grabbed;
	}
	public String getGrabbable() {
		return grabbable;
	}
	public void setGrabbable(String grabbable) {
		this.grabbable = grabbable;
	}
	public String getClasses() {
		return classes;
	}
	public void setClasses(String classes) {
		this.classes = classes;
	}
	
}
