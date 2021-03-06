package com.sarxos.medusa.market;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Signal class.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class Signal {

	public static class Value {
		
		private String name = null;
		
		private double value = 0;

		public Value(String name, double value) {
			super();
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public double getValue() {
			return value;
		}
	}
	
	/**
	 * Signal type - buy/sell/delay/wait
	 */
	private SignalType type = null;

	/**
	 * Signal generation date.
	 */
	private Date date = null;

	/**
	 * Quote element on which signal has been generated
	 */
	private Quote quote = null;

	/**
	 * Numeric value of signal trigger - need to be removed/reworked?
	 */
	private double level = 0;

	private List<Value> values = null; 
	
	public Signal() {
		super();
	}

	public Signal(Quote quote, SignalType type) {
		this(quote.getDate(), type, quote, 0);
	}

	public Signal(Date date, SignalType type) {
		this(date, type, null, 0);
	}

	public Signal(Date date, SignalType type, Quote quote) {
		this(date, type, quote, 0);
	}

	public Signal(Date date, SignalType type, Quote quote, double level) {
		super();
		this.date = date;
		this.type = type;
		this.quote = quote;
		this.level = level;
	}

	public Quote getQuote() {
		return quote;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	public SignalType getType() {
		return type;
	}

	public void setType(SignalType type) {
		this.type = type;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName());
		sb.append("[");
		sb.append(Quote.DATE_FORMAT.format(getDate()));
		sb.append(" ");
		sb.append(getType());
		sb.append("]");
		return sb.toString();
	}

	public double getLevel() {
		return level;
	}

	public void setLevel(double level) {
		this.level = level;
	}
	
	private void checkList() {
		if (values == null) {
			values = new LinkedList<Value>();
		}		
	}
	
	public void addValue(Value value) {
		checkList();
		values.add(value);
	}
	
	public void addValues(Collection<Value> values) {
		checkList();
		this.values.addAll(values);
	}
	
	public List<Value> getValues() {
		return values;
	}
}
