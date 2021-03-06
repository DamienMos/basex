package org.basex.query;

import static org.basex.core.Text.*;

import java.util.*;

import org.basex.data.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Thrown to indicate an exception during the parsing or evaluation of a query.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public class QueryException extends Exception {
  /** Stack. */
  private final ArrayList<InputInfo> stack = new ArrayList<InputInfo>();
  /** Error QName. */
  private final QNm name;
  /** Error value. */
  private Value value = Empty.SEQ;
  /** Error reference. */
  private Err err;
  /** Code suggestions. */
  private StringList suggest;
  /** Error line and column. */
  private InputInfo info;
  /** Marked error column. */
  private int markedCol;
  /** Marks if this exception is catchable by a {@code try/catch} expression. */
  private boolean catchable = true;

  /**
   * Constructor, specifying an exception or error. {@link Err#BASX_GENERIC} will be set
   * as error code.
   * @param th exception or error
   */
  public QueryException(final Throwable th) {
    this(Util.message(th));
  }

  /**
   * Constructor, specifying a simple error message. {@link Err#BASX_GENERIC} will be set
   * as error code.
   * @param msg error message
   */
  public QueryException(final String msg) {
    this(null, Err.BASX_GENERIC, msg);
  }

  /**
   * Default constructor.
   * @param ii input info
   * @param er error reference
   * @param ext error extension
   */
  public QueryException(final InputInfo ii, final Err er, final Object... ext) {
    this(ii, er.qname(), er.desc, ext);
    err = er;
  }

  /**
   * Constructor, specifying the error code and message as string.
   * @param ii input info
   * @param errc error code
   * @param msg error message
   * @param ext error extension
   */
  public QueryException(final InputInfo ii, final QNm errc, final String msg, final Object... ext) {
    super(message(msg, ext));
    name = errc;
    if(ii != null) info(ii);
    for(final Object o : ext) {
      if(o instanceof Throwable) {
        initCause((Throwable) o);
        break;
      }
    }
  }

  /**
   * Returns the error column.
   * @return error column
   */
  public int column() {
    return info == null ? 0 : info.column();
  }

  /**
   * Returns the marked error column.
   * @return marked error column
   */
  public int markedColumn() {
    return markedCol;
  }

  /**
   * Returns the error line.
   * @return error line
   */
  public int line() {
    return info == null ? 0 : info.line();
  }

  /**
   * Returns the file.
   * @return error line
   */
  public String file() {
    return info == null ? null : info.path();
  }

  /**
   * Returns suggestions for code suggestions.
   * @return suggestions
   */
  public StringList suggest() {
    return suggest == null ? new StringList() : suggest;
  }

  /**
   * Sets code suggestions.
   * @param qp query parser
   * @param sug code suggestions
   * @return self reference
   */
  public QueryException suggest(final InputParser qp, final StringList sug) {
    suggest = sug;
    pos(qp);
    return this;
  }

  /**
   * Adds an input info to the stack.
   * @param ii input info
   */
  public void add(final InputInfo ii) {
    if(ii != null) stack.add(ii);
  }

  /**
   * Sets input info.
   * @param ii input info
   * @return self reference
   */
  public QueryException info(final InputInfo ii) {
    info = ii;
    return this;
  }

  /**
   * Returns the input info.
   * @return input info
   */
  public InputInfo info() {
    return info;
  }

  /**
   * Sets the error value.
   * @param v error value
   * @return self reference
   */
  public QueryException value(final Value v) {
    value = v;
    return this;
  }

  /**
   * Sets an error.
   * @param e error
   * @return self reference
   */
  public QueryException err(final Err e) {
    err = e;
    return this;
  }

  /**
   * Finds line and column for the specified query parser.
   * @param parser parser
   */
  void pos(final InputParser parser) {
    markedCol = parser.mark;
    if(info != null) return;
    // check if line/column information has already been added
    parser.pos = Math.min(parser.mark, parser.length);
    info = new InputInfo(parser);
  }

  /**
   * Returns the error name.
   * @return error name
   */
  public QNm qname() {
    return name;
  }

  /**
   * Returns the error.
   * @return error
   */
  public Err err() {
    return err;
  }

  /**
   * Returns the error value.
   * @return error value
   */
  public Value value() {
    return value;
  }

  @Override
  public String getLocalizedMessage() {
    return super.getMessage();
  }

  @Override
  public String getMessage() {
    final TokenBuilder tb = new TokenBuilder();
    if(info != null) tb.add(STOPPED_AT).add(info.toString()).add(COL).add(NL);
    final byte[] code = name.local();
    if(code.length != 0) tb.add('[').add(code).add("] ");
    tb.add(getLocalizedMessage());
    if(!stack.isEmpty()) {
      tb.add(NL).add(NL).add(STACK_TRACE).add(COL);
      for(final InputInfo ii : stack) tb.add(NL).add(LI).add(ii.toString());
    }
    return tb.toString();
  }

  /**
   * Checks if this exception can be caught by a {@code try/catch} expression.
   * @return result of check
   */
  public boolean isCatchable() {
    return catchable;
  }

  /**
   * Makes this exception uncatchable by a {@code try/catch} expression.
   * @return self reference for convenience
   */
  public QueryException notCatchable() {
    catchable = false;
    return this;
  }

  /**
   * Creates the error message from the specified text and extension array.
   * @param text text message with optional placeholders
   * @param ext info extensions
   * @return argument
   */
  private static String message(final String text, final Object[] ext) {
    final int es = ext.length;
    for(int e = 0; e < es; e++) {
      Object o = ext[e];
      if(o instanceof byte[]) {
        o = Token.string((byte[]) o);
      } else if(o instanceof ExprInfo) {
        o = ((ExprInfo) o).toString(Integer.MAX_VALUE);
      } else if(o instanceof Throwable) {
        o = Util.message((Throwable) o);
      } else if(!(o instanceof String)) {
        o = String.valueOf(o);
      }
      ext[e] = o;
    }
    return Util.info(text, ext);
  }
}
