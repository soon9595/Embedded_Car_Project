#include <linux/kernel.h>
   
asmlinkage long sys_car_data(int direction, int mode)
{
	return (direction<<16) +  (mode);
}
   
