import java.util.*;
import java.util.regex.*;

/**
 * See assignment handout for the grammar.
 * You need to implement the parse(..) method and all the rest of the parser.
 * There are several methods provided for you:
 * - several utility methods to help with the parsing
 * See also the TestParser class for testing your code.
 */
public class Parser {


    // Useful Patterns

    static final Pattern NUMPAT = Pattern.compile("-?[1-9][0-9]*|0"); 
    static final Pattern OPENPAREN = Pattern.compile("\\(");
    static final Pattern CLOSEPAREN = Pattern.compile("\\)");
    static final Pattern OPENBRACE = Pattern.compile("\\{");
    static final Pattern CLOSEBRACE = Pattern.compile("\\}");
    static final Pattern ACTPAT = Pattern.compile("move|turnL|turnR|turnAround|shieldOn|shielfOff|takeFuel|wait");
    static final Pattern MOVEPAT = Pattern.compile("move");
    static final Pattern LEFTPAT = Pattern.compile("turnL");
    static final Pattern RIGHTPAT = Pattern.compile("turnR");
    static final Pattern TAKEPAT = Pattern.compile("takeFuel");
    static final Pattern WAITPAT = Pattern.compile("wait");
    static final Pattern LOOPPAT = Pattern.compile("loop");


    //----------------------------------------------------------------
    /**
     * The top of the parser, which is handed a scanner containing
     * the text of the program to parse.
     * Returns the parse tree.
     */

    //Grammer: PROG ::= STMT* ::= ACT ";" | LOOP
    ProgramNode parse(Scanner s) {
        // Set the delimiter for the scanner.
        s.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");
        // THE PARSER GOES HERE
        // Call the parseProg method for the first grammar rule (PROG) and return the node
        if(!s.hasNext()){
            fail("Nothing is here!", s);
        }
        List<ProgramNode> nodeList = new ArrayList<ProgramNode>();
        //need to loop this untill no more stuff to loop!
        while(s.hasNext()){
            ProgramNode programN = parseStatement(s);
            if(programN != null){
                nodeList.add(programN);
            }
        }
        
        return new ProgramNode() {
            @Override
            public void execute(Robot robot) {
                for(ProgramNode n : nodeList){
                    n.execute(robot);
                }
            }
        };


         //return null;
        
    }


    ProgramNode parseStatement(Scanner s){
         if(s.hasNext(ACTPAT)){
            ProgramNode act = actionParse(s);
            require(";", "No semi collon found", s);
            return act;
        }
        if(s.hasNext(LOOPPAT)){
            return loopParse(s);
        }
        fail("statement neither action or loop", s);
        return null;
    }

    
    ProgramNode actionParse(Scanner s){
        if(s.hasNext(MOVEPAT)){
            return parseMove(s);
        }
        if(s.hasNext(LEFTPAT)){
            return parseLeft(s);
        }
        if(s.hasNext(RIGHTPAT)){
            return parseRight(s);
        }
        if(s.hasNext(TAKEPAT)){
            return parseTake(s);
        }
        if(s.hasNext(WAITPAT)){
            return parseWait(s);
        }
        return null;
    }

    ProgramNode parseMove(Scanner s){
        require(MOVEPAT,"need 'move'",s);
            return new MoveNode();
        
    }
    ProgramNode parseLeft(Scanner s){
        require(LEFTPAT,"need 'turn:'",s);
            return new LeftNode();
        
    }
    ProgramNode parseRight(Scanner s){
        require(RIGHTPAT,"need 'turnR'",s);
            return new RightNode();
        
    }
    ProgramNode parseTake(Scanner s){
        require(TAKEPAT,"need 'takeFuel'",s);
            return new FuelNode();
        
    }
    ProgramNode parseWait(Scanner s){
        require(WAITPAT,"need 'wait'",s);
            return new WaitNode();
        
    }

    //Grammer: LOOP ::= "loop" BLOCK
    LoopNode loopParse(Scanner s){
        require(LOOPPAT, "no 'loop' found", s);
        ProgramNode block = parseBlock(s);
        if(block == null){
            return null;
        }

        return new LoopNode(block);

    }

    ProgramNode parseBlock(Scanner s){
        List<ProgramNode> blocks = new ArrayList<ProgramNode>();
        require(OPENBRACE, "expecting opening bracket", s);
        //STMT
        while(!s.hasNext(CLOSEBRACE)){
            blocks.add(parseStatement(s));
        }
        require(CLOSEBRACE,"expecting ending bracket", s);

        return new BlockNode(blocks);

   
    }



    //----------------------------------------------------------------
    // utility methods for the parser
    // - fail(..) reports a failure and throws exception
    // - require(..) consumes and returns the next token as long as it matches the pattern
    // - requireInt(..) consumes and returns the next token as an int as long as it matches the pattern
    // - checkFor(..) peeks at the next token and only consumes it if it matches the pattern

    /**
     * Report a failure in the parser.
     */
    static void fail(String message, Scanner s) {
        String msg = message + "\n   @ ...";
        for (int i = 0; i < 5 && s.hasNext(); i++) {
            msg += " " + s.next();
        }
        throw new ParserFailureException(msg + "...");
    }

    /**
     * Requires that the next token matches a pattern if it matches, it consumes
     * and returns the token, if not, it throws an exception with an error
     * message
     */
    static String require(String p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    static String require(Pattern p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    /**
     * Requires that the next token matches a pattern (which should only match a
     * number) if it matches, it consumes and returns the token as an integer
     * if not, it throws an exception with an error message
     */
    static int requireInt(String p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    static int requireInt(Pattern p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    /**
     * Checks whether the next token in the scanner matches the specified
     * pattern, if so, consumes the token and return true. Otherwise returns
     * false without consuming anything.
     */
    static boolean checkFor(String p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;}
        return false;
    }

    static boolean checkFor(Pattern p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;} 
        return false;
    }

}



 

// You could add the node classes here or as separate java files.
// (if added here, they must not be declared public or private)
// For example:
//  class BlockNode implements ProgramNode {.....
//     with fields, a toString() method and an execute() method
//


interface ProgramNode{
    public void execute(Robot robot);
}

class StatementNode implements ProgramNode{
    //value
    //constructor
    final StatementNode stmt;
    public StatementNode(StatementNode stmt){
        this.stmt = stmt;
    }
    public void execute(Robot robot){}
}
class ActionNode implements ProgramNode{
    final ActionNode action;
    public ActionNode(ActionNode action){
        this.action = action;
    }
    public void execute(Robot robot){}
}

class LoopNode implements ProgramNode{
    final ProgramNode stmt;
    public LoopNode(ProgramNode stmt){
        this.stmt = stmt;
    }
    public void execute(Robot robot){
        this.stmt.execute(robot);

    }
}
class BlockNode implements ProgramNode{

    final List<ProgramNode> blocks;
    public BlockNode(List<ProgramNode> blocks){
        this.blocks = blocks;
    }
    public void execute(Robot robot){
        while(true){
            for(ProgramNode block : blocks){
                block.execute(robot);
            }
    }
    }
    public String toString(){
        String str = " ";
        for(ProgramNode block:blocks){
            str = str + block.toString();
        }
        return str;
    }




}

    

class MoveNode implements ProgramNode{
    public MoveNode(){}
    public String toString(){
        return "move";
    }
    public void execute(Robot robot){
        //if move is 5 need a for loop that call it 5 times
        robot.move();
    }
}

class LeftNode implements ProgramNode{
    public LeftNode(){}
    public String toString(){
        return "turnL";
    }
    public void execute(Robot robot){
        //if move is 5 need a for loop that call it 5 times
        robot.turnLeft();
    }
}

class RightNode implements ProgramNode{
    public RightNode(){}
    public String toString(){
        return "turnR";
    }
    public void execute(Robot robot){
        //if move is 5 need a for loop that call it 5 times
        robot.turnRight();
    }
}

class WaitNode implements ProgramNode{
    public WaitNode(){}
    public String toString(){
        return "wait";
    }
    public void execute(Robot robot){
        //if move is 5 need a for loop that call it 5 times
        robot.idleWait();
    }
}


class FuelNode implements ProgramNode{
    public FuelNode(){}
    public String toString(){
        return "takeFuel";
    }
    public void execute(Robot robot){
        //if move is 5 need a for loop that call it 5 times
        robot.takeFuel();
    }
}