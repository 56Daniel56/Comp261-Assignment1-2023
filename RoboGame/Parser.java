import java.util.*;
import java.util.concurrent.locks.Condition;
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
    static final Pattern ACTPAT = Pattern.compile("move|turnL|turnR|turnAround|shieldOn|shieldOff|takeFuel|wait");
    static final Pattern MOVEPAT = Pattern.compile("move");
    static final Pattern LEFTPAT = Pattern.compile("turnL");
    static final Pattern RIGHTPAT = Pattern.compile("turnR");
    static final Pattern TAKEPAT = Pattern.compile("takeFuel");
    static final Pattern WAITPAT = Pattern.compile("wait");
    static final Pattern LOOPPAT = Pattern.compile("loop");
    static final Pattern IFPAT = Pattern.compile("if");
    static final Pattern WHILEPAT= Pattern.compile("while");
    static final Pattern COMMAPAT= Pattern.compile(",");
    static final Pattern SENSPAT= Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");
    static final Pattern FUELLEFTPAT= Pattern.compile("fuelLeft");
    static final Pattern OPPLRPAT= Pattern.compile("oppLR");
    static final Pattern OPPFBPAT= Pattern.compile("oppFB");
    static final Pattern NUMBARPAT= Pattern.compile("numBarrels");
    static final Pattern BARLRPAT= Pattern.compile("barrelLR");
    static final Pattern BARFBPAT= Pattern.compile("barrelFB");
    static final Pattern WALLDISTPAT= Pattern.compile("wallDist");
    static final Pattern SHIELDONPAT= Pattern.compile("shieldOn");
    static final Pattern SHIELDOFFPAT= Pattern.compile("shieldOff");
    static final Pattern TURNAROUNDPAT= Pattern.compile("turnAround");


    



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
            else{fail("something bad happened with the statement!",s);}
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
            if(act==null){
                return null;
            }
            return act;
        }
        if(s.hasNext(LOOPPAT)){

            ProgramNode loop = loopParse(s);
            if(loop == null){
                return null;
            }
            return loop;
        }

        if(s.hasNext(IFPAT)){
            ProgramNode ifParse =ifParse(s);
            if(ifParse != null){
                return ifParse;
            }
        }

        if(s.hasNext(WHILEPAT)){
            ProgramNode whileParse = whileParse(s);
            if(whileParse != null){
                return whileParse;
            }
            
        }

        fail("issue parsing current statement!", s);
        return null;
    }

    ProgramNode whileParse(Scanner s){
        require(WHILEPAT, "'while' is required", s);
        require(OPENPAREN, "open bracket required", s);
        CondNode cond = condParse(s);
        require(CLOSEPAREN, "close bracket required", s);
        ProgramNode block = parseBlock(s);
        return new ProgramNode() {
            public void execute(Robot robot) {
                while(cond.execute(robot) == true){
                    block.execute(robot);
                }
            }
        };
    }

    ProgramNode ifParse(Scanner s){
        require(IFPAT,"'if' is required",s);
        require(OPENPAREN, "open bracket required", s);
        CondNode cond = condParse(s);
        require(CLOSEPAREN, "close bracket required", s);
        ProgramNode block = parseBlock(s);

        return new ProgramNode() {
            public void execute(Robot robot) {
                if(cond.execute(robot)){
                    block.execute(robot);
                }
            }
        };
    }

    CondNode condParse(Scanner s){
        
        BooleanNode relop = relopParse(s);

        require(OPENPAREN,"required open bracket",s);
        SensNode sens = null;
        if(s.hasNext(SENSPAT)){
            sens = parseSens(s);
        }
        require(COMMAPAT, "comma required", s);
        int num = numParse(s);
        require(CLOSEPAREN, "close bracket required", s);
        
        final SensNode sen = sens;
        return new CondNode() {
           
            public boolean execute(Robot robot) {
                return relop.evaluate(sen.evaluate(robot),num);
            }
            
        };
        //return relop.evaluate(sens.getValue(),num); 
    }

    int numParse(Scanner s){
        int num = requireInt(NUMPAT, "Number needs to be between correct range", s);
        
        return num;
    }

    SensNode parseSens(Scanner s){
        if(s.hasNext(FUELLEFTPAT)){
            require(FUELLEFTPAT, "fuelleft rquired", s);
            return new FuelLeftNode();
        }
        else if(s.hasNext(OPPLRPAT)){
            //return parseOppLr(s);
            require(OPPLRPAT, "require opponent lr ", s);
            return new OpponentLR();
        }   
        else if(s.hasNext(OPPFBPAT)){
            //return parseOppFb(s);
            require(OPPFBPAT, "require opponent fb ", s);
            return new OpponentFB();
        }   
        else if(s.hasNext(NUMBARPAT)){
            require(NUMBARPAT, "required number pattern tokken", s);
            return new NumberBarrelNode();
        }  
        else if(s.hasNext(BARLRPAT)){
           // return parseBarLr(s);
           require(BARLRPAT, "Requires barrellrpat", s);
           return new BarrelLRNode();
        }
        else if(s.hasNext(BARFBPAT)){
           // return parseBarFb(s);
           //fail("this barrelfb method not fully implemented!",s);
           require(BARFBPAT, "barrelfb required", s);
           return new BarrelFBNode();
        }
        else if(s.hasNext(WALLDISTPAT)){
            //return parseWallDist(s);
            require(WALLDISTPAT, "requiring wall dist pat", s);
            return new WallDistNode();
        }
        else{
            fail("parseSens parameter missing!", s);
        }
        
        
        return null;
    }
    
    BooleanNode relopParse(Scanner s){
        if(s.hasNext("lt")){
            require("lt",  "missing eq symbol", s);
            return BooleanNode.lt;
       }
        if(s.hasNext("gt")){
            require("gt",  "missing gt symbol", s);
            return BooleanNode.gt;
        }
        if(s.hasNext("eq")){
            require("eq",  "missing eq symbol", s);
            return BooleanNode.eq;
        }
        
        
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
        if(s.hasNext(SHIELDONPAT)){
            require("shieldOn", "requires shield on", s);
            return new ProgramNode() {
                public void execute(Robot robot){
                    robot.setShield(true);
                }
                public String toString(){
                    return "shield on";
                }
            };
        }
        if(s.hasNext(SHIELDOFFPAT)){
            require("shieldOff", "requires shield off", s);
            return new ProgramNode() {
                public void execute(Robot robot){
                    robot.setShield(false);
                }
                public String toString(){
                    return "shield off";
                }
            };
            }
        if(s.hasNext(TURNAROUNDPAT)){
            require(TURNAROUNDPAT, "requires turn around pat", s);
            return new TurnAroundNode();
        }
        return null;
    }

    ProgramNode parseMove(Scanner s){
        require(MOVEPAT,"need 'move'",s);
        ProgramNode move = new MoveNode();
        if(!s.hasNext(";")){
            require(OPENPAREN, "open bracket required if no ; right away", s);
            
        }
        return move;
        
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



    ProgramNode parseFuelLeft(Scanner s){
        require("fuelLeft","fuel left needed!",s);
        //return new FuelLeftNode().howMuch();
        return null;

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
        if(blocks.isEmpty()){
            fail("Block had no tokens that it could take", s);
        }
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

interface CondNode{
    boolean execute(Robot robot);
}

interface BooleanNode{
   public boolean evaluate(int a, int b);

   BooleanNode eq = new BooleanNode(){
    public boolean evaluate(int a, int b) {
        return a==b;

    }
    public String toString(){
        return "eq";
    }
};
BooleanNode lt = new BooleanNode(){
    public boolean evaluate(int a, int b) {
        return a<b;

    }
    public String toString(){
        return "lt";
    }
};
BooleanNode gt = new BooleanNode(){
    public boolean evaluate(int a, int b) {
        return a>b;
        }
        public String toString(){
            return "gt";
        }
 };  



}


interface ExprNode{
    public int evaluate(Robot robot);
}


interface SensNode extends ExprNode{
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
        while(true){
            this.stmt.execute(robot);
        }
        

    }
}
class BlockNode implements ProgramNode{

    final List<ProgramNode> blocks;
    public BlockNode(List<ProgramNode> blocks){
        this.blocks = blocks;
    }
    public void execute(Robot robot){

            for(ProgramNode block : blocks){
                block.execute(robot);
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



class FuelLeftNode implements SensNode{
    public FuelLeftNode(){}
   
    public int evaluate(Robot robot){
        return robot.getFuel();
    }
    public String toString(){
        return "fuelleft";
    }

}
class TurnAroundNode implements ProgramNode{
    public void execute(Robot robot){
        robot.turnAround();
    }
    public String toString(){
        return "turn around node";
    }
}

class NumberBarrelNode implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.numBarrels();
        return this.value;
    }

    public String toString(){
        return "number of barrels";
    }
}

class BarrelFBNode implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getBarrelFB(0);
        return this.value;
    }

    public String toString(){
        return "number of barrels";
    }
}

class BarrelLRNode implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getBarrelLR(0);
        return this.value;
    }

    public String toString(){
        return "number of barrels";
    }
}

class WallDistNode implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value =  robot.getDistanceToWall();
        return this.value;
    }
    public String toString(){
        return "dist to wall!";
    }
}

class OpponentLR implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getOpponentLR();
        return this.value;
    }
    public String toString(){
        return "opponent LR... whatever that means?";
    }
}

class OpponentFB implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getOpponentFB();
        return this.value;
    }
    public String toString(){
        return "opponent FB... whatever that means?";
    }
}

class ClosestBarrelFB implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getClosestBarrelFB();
        return this.value;
    }
    public String toString(){
        return "clossest barrel FB";
    }
}

class ClosestBarrelLR implements SensNode{
    int value;
    public int evaluate(Robot robot){
        this.value = robot.getClosestBarrelLR();
        return this.value;
    }
    public String toString(){
        return "clossest barrel LR";
    }
}