------------------------------- MODULE Economy -------------------------------
\* TLA+ model of the ServerTools economy, mirroring spec/economy.qnt and the Java
\* EconomyStorage. Balances are whole-cent integers. Every operation is guarded
\* exactly like the code, and the disjunct `UNCHANGED balances` models a rejected
\* input (0, negative, self-pay, overflow) leaving state untouched.
\*
\* Check with TLC:
\*   tlc -config Economy_safe.tla.cfg      Economy.tla   \* NonNegative /\ UnderLimit
\*   tlc -config Economy_conserved.tla.cfg Economy.tla   \* money conserved under pay
EXTENDS Integers, FiniteSets

CONSTANTS Accounts, MaxCents, Start

VARIABLE balances

RECURSIVE SumSet(_)
SumSet(S) == IF S = {} THEN 0
             ELSE LET x == CHOOSE y \in S : TRUE
                  IN balances[x] + SumSet(S \ {x})

Total == SumSet(Accounts)

\* amounts a caller might submit: includes 0, negatives, and an over-limit value
Amounts == (-2)..(MaxCents + 1)

Init == balances = [a \in Accounts |-> Start]

Pay(from, to, amt) ==
  /\ amt > 0
  /\ from # to
  /\ balances[from] >= amt
  /\ balances[to] + amt <= MaxCents
  /\ balances' = [balances EXCEPT ![from] = @ - amt, ![to] = @ + amt]

Deposit(a, amt) ==
  /\ amt >= 0
  /\ balances[a] + amt <= MaxCents
  /\ balances' = [balances EXCEPT ![a] = @ + amt]

Withdraw(a, amt) ==
  /\ amt >= 0
  /\ balances[a] >= amt
  /\ balances' = [balances EXCEPT ![a] = @ - amt]

SetBal(a, amt) ==
  /\ amt >= 0
  /\ amt <= MaxCents
  /\ balances' = [balances EXCEPT ![a] = amt]

Next ==
  \/ \E from \in Accounts, to \in Accounts, amt \in Amounts : Pay(from, to, amt)
  \/ \E a \in Accounts, amt \in Amounts : Deposit(a, amt)
  \/ \E a \in Accounts, amt \in Amounts : Withdraw(a, amt)
  \/ \E a \in Accounts, amt \in Amounts : SetBal(a, amt)
  \/ UNCHANGED balances                            \* invalid input -> no-op

NextPayOnly ==
  \/ \E from \in Accounts, to \in Accounts, amt \in Amounts : Pay(from, to, amt)
  \/ UNCHANGED balances

NonNegative == \A a \in Accounts : balances[a] >= 0
UnderLimit  == \A a \in Accounts : balances[a] <= MaxCents
Safe        == NonNegative /\ UnderLimit

Conserved == Total = Cardinality(Accounts) * Start
==============================================================================
