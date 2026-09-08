// Driver for CppRoundTripFuncTest - compiled together with the generated C++ parser.
#include "CalcTokenManager.h"
#include "Calc.h"
#include <cstdio>

static int eval (const JJString& sInput)
{
  CharStream cs (sInput.c_str (), sInput.size (), 1, 1);
  CalcTokenManager tm (&cs);
  Calc p (&tm);
  return p.sum ();
}

int main ()
{
  printf ("%d\n", eval ("1 + 2 + 39"));
  printf ("%d\n", eval ("2 * 3 + 4 * 10"));
  printf ("%d\n", eval ("7"));
  return 0;
}
