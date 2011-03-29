package com.sarxos.medusa.trader;

import static com.sarxos.medusa.market.SignalType.BUY;
import static com.sarxos.medusa.market.SignalType.SELL;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sarxos.medusa.comm.MessagesBroker;
import com.sarxos.medusa.comm.MessagingException;
import com.sarxos.medusa.data.persistence.Persistent;
import com.sarxos.medusa.market.Paper;
import com.sarxos.medusa.market.Position;
import com.sarxos.medusa.market.Quote;
import com.sarxos.medusa.market.SignalGenerator;
import com.sarxos.medusa.market.SignalType;
import com.sarxos.medusa.market.Symbol;
import com.sarxos.medusa.provider.Providers;
import com.sarxos.medusa.provider.RealTimeProvider;


/**
 * Trader class. It is designed to handle decision events from decision maker,
 * send notification/question and buy or sell given paper.
 * 
 * @author Bartosz Firyn (SarXos)
 */
@Persistent("trader")
public abstract class Trader implements DecisionListener, Runnable {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Trader.class.getSimpleName());

	/**
	 * Set of signal types to be acknowledged by player.
	 */
	public static final Set<SignalType> NOTIFICATIONS = Collections.unmodifiableSet(EnumSet.of(BUY, SELL));

	/**
	 * Decision maker (encapsulate decision logic).
	 */
	private DecisionMaker decisionMaker = null;

	/**
	 * Signal generator to use.
	 */
	@Persistent
	private SignalGenerator<? extends Quote> siggen = null;

	/**
	 * Real time data provider.
	 */
	private RealTimeProvider provider = null;

	/**
	 * Trader name.
	 */
	private final String name;

	/**
	 * Current position (long, short).
	 */
	@Persistent
	private Position position = null;

	/**
	 * Trader's paper quantity.
	 */
	@Persistent
	private int paperQuantity = 0;

	/**
	 * Trader's paper desired quantity (how many papers I want buy)
	 */
	@Persistent
	private int paperDesiredQuantity = 0;

	/**
	 * Messaging broker.
	 */
	private MessagesBroker broker = null;

	private Paper paper = null;

	/**
	 * Trader constructor.
	 * 
	 * @param name - trader name
	 * @param siggen - signal generator
	 * @param paper - paper to observe
	 */
	public Trader(String name, SignalGenerator<? extends Quote> siggen, Paper paper) {
		this(name, siggen, paper, null);
	}

	@Persistent
	public Trader(String name, SignalGenerator<? extends Quote> siggen, Paper paper, RealTimeProvider provider) {
		if (name == null) {
			throw new IllegalArgumentException("Trader name cannot be null");
		}
		if (siggen == null) {
			throw new IllegalArgumentException("Signal generator cannot be null");
		}
		if (paper == null) {
			throw new IllegalArgumentException("Paper cannot be null");
		}
		this.name = name;
		this.siggen = siggen;
		this.provider = provider;
		this.paper = paper;
		this.init();
	}

	/**
	 * Initialize trader
	 */
	protected void init() {

		if (provider == null) {
			provider = Providers.getRealTimeProvider();
		}

		Observer observer = new Observer(provider, paper.getSymbol());
		DecisionMaker dm = new DecisionMaker(observer, siggen);

		dm.setTrader(this);
		dm.addDecisionListener(this);

		setDecisionMaker(dm);

		try {
			broker = new MessagesBroker();
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void positionChange(PositionEvent pe) {
		Position p = pe.getNewPosition();
		if (position != p) {
			position = p;
		}
	}

	/**
	 * Set new position
	 * 
	 * @param p - new position to set
	 */
	public void setPosition(Position p) {
		if (p == null) {
			throw new IllegalArgumentException("Position cannot be null");
		}
		if (position != p) {
			position = p;
		}
		getDecisionMaker().setPosition(p);
	}

	/**
	 * @return Return current position (long, short)
	 */
	public Position getPosition() {
		if (position == null) {
			DecisionMaker dm = getDecisionMaker();
			if (dm == null) {
				return null;
			}
			return dm.getCurrentPosition();
		} else {
			return position;
		}
	}

	/**
	 * @return Decision maker
	 */
	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	/**
	 * Set new decision maker
	 * 
	 * @param decisionMaker - new decision maker to set
	 */
	public void setDecisionMaker(DecisionMaker decisionMaker) {
		this.decisionMaker = decisionMaker;
	}

	/**
	 * @return Price observer
	 */
	public Observer getObserver() {
		if (getDecisionMaker() == null) {
			return null;
		}
		return getDecisionMaker().getObserver();
	}

	/**
	 * Set new price observer.
	 * 
	 * @param observer - new observer to set
	 */
	public void setObserver(Observer observer) {
		if (getDecisionMaker() == null) {
			throw new IllegalStateException(
				"Cannot set observer because decision maker is not " +
				"created.");
		}
		getDecisionMaker().setObserver(observer);
	}

	/**
	 * @return Observed symbol (e.g. KGH, BRE)
	 */
	public Symbol getSymbol() {
		DecisionMaker dm = getDecisionMaker();
		Observer o = null;
		if (dm != null && (o = dm.getObserver()) != null) {
			return o.getSymbol();
		} else {
			return paper.getSymbol();
		}
	}

	/**
	 * @return Signal generator class name
	 */
	public String getGeneratorClassName() {
		return siggen.getClass().getName();
	}

	/**
	 * @return Signal generator
	 */
	public SignalGenerator<? extends Quote> getGenerator() {
		return siggen;
	}

	/**
	 * @return Trader's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Start trade.
	 * 
	 * @param symbol - observed symbol
	 */
	public void trade() {
		getDecisionMaker().getObserver().start();
	}

	@Override
	public void run() {
		trade();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName()).append('[');
		sb.append(getSymbol()).append(' ');
		sb.append(getPosition()).append(' ');
		sb.append(paper.getQuantity()).append('/').append(paper.getDesiredQuantity()).append(']');
		sb.append('[').append(getGenerator().getClass().getSimpleName()).append(']');
		return sb.toString();
	}

	/**
	 * @return Messages broker
	 */
	public MessagesBroker getMessagesBroker() {
		return broker;
	}

	/**
	 * Set new messages broker.
	 * 
	 * @param broker - new messages broker to set
	 */
	public void setMessagesBroker(MessagesBroker broker) {
		if (broker == null) {
			throw new IllegalArgumentException("Messages broker cannot be null");
		}
		this.broker = broker;
	}

	/**
	 * Acknowledge player about decision maker's market decision. This method is
	 * blocking - it won't return any value till user acknowledgment response.
	 * 
	 * @param de - decision event
	 * @return true if user acknowledged, false otherwise
	 */
	public boolean acknowledge(DecisionEvent de) {

		SignalType signal = de.getSignalType();
		Paper paper = de.getPaper();

		if (NOTIFICATIONS.contains(signal)) {

			if (LOG.isInfoEnabled()) {
				LOG.info("Acknowledge decision " + de);
			}

			boolean ok = false;
			try {
				ok = getMessagesBroker().acknowledge(paper, signal);
			} catch (MessagingException e) {
				LOG.error("Cannot acknowledge decision", e);
			}

			if (LOG.isInfoEnabled()) {
				LOG.info("Aknowledge response is " + ok);
			}

			return ok;
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug(
					"This kind of decision (" + de.getSignalType() +
					") cannot be acknowledge");
			}
		}

		return false;
	}

	/**
	 * @return the paperQuantity
	 */
	public int getPaperQuantity() {
		int pq = paper.getQuantity();
		if (pq != paperQuantity) {
			setPaperQuantity(pq);
		}
		return pq;
	}

	/**
	 * @param quantity - the paper quantity to set
	 */
	public void setPaperQuantity(int quantity) {
		if (paper == null) {
			throw new RuntimeException("Trader's paper cannot be null!");
		}
		paper.setQuantity(quantity);
		paperQuantity = quantity;
	}

	/**
	 * @return the paper desired quantity
	 */
	public int getPaperDesiredQuantity() {
		int pdq = paper.getDesiredQuantity();
		if (pdq != paperDesiredQuantity) {
			setPaperDesiredQuantity(pdq);
		}
		return pdq;
	}

	/**
	 * @param desired - desired paper quantity
	 */
	public void setPaperDesiredQuantity(int desired) {
		if (paper == null) {
			throw new RuntimeException("Trader's paper cannot be null!");
		}
		paper.setDesiredQuantity(desired);
		paperDesiredQuantity = desired;
	}

	/**
	 * @return Return paper.
	 */
	public Paper getPaper() {
		return paper;
	}
}
