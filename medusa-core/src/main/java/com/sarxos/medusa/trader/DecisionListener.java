package com.sarxos.medusa.trader;

/**
 * Decision listener interface.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public interface DecisionListener {

	/**
	 * Notified after decision change.
	 * 
	 * @param de - decision event
	 */
	public void decisionChange(DecisionEvent de);
}
