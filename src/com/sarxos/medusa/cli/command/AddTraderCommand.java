package com.sarxos.medusa.cli.command;

import org.naturalcli.Command;
import org.naturalcli.ExecutionException;
import org.naturalcli.ICommandExecutor;
import org.naturalcli.InvalidSyntaxException;
import org.naturalcli.ParseResult;

import com.sarxos.medusa.data.DBDAO;
import com.sarxos.medusa.data.DBDAOException;
import com.sarxos.medusa.generator.MAVD;
import com.sarxos.medusa.market.Symbol;
import com.sarxos.medusa.trader.Trader;


/**
 * Print list of dates with missing quotes for given symbol.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class AddTraderCommand extends Command implements ICommandExecutor {

	protected static final String syntax = "trade <symbol:string> <name:string>";

	protected static final String help = "Add trader";

	public AddTraderCommand() throws InvalidSyntaxException {
		prepare(syntax, help, this);
	}

	@Override
	public void execute(ParseResult pr) throws ExecutionException {

		String symbol_str = (String) pr.getParameterValue(0);
		Symbol symbol = null;

		try {
			symbol = Symbol.valueOf(symbol_str);
		} catch (IllegalArgumentException e) {
			System.err.println("Paper '" + symbol_str + "' is not supported.");
		}

		if (symbol == null) {
			return;
		}

		String name = (String) pr.getParameterValue(1);

		Trader t = new Trader(name, new MAVD(3, 13, 30), symbol);

		try {
			DBDAO.getInstance().addTrader(t);
		} catch (DBDAOException e) {
			e.printStackTrace();
		}
	}
}
