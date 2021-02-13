#include <linux/init.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/pci.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <linux/uaccess.h>
#include <linux/sched.h>
#include <linux/dma-mapping.h>
#include <linux/types.h>
#include <linux/cma.h>
#define AXIBAR0_U 0x208
#define AXIBAR0_L 0x20C

// Xilinx
#define VENDOR_ID 0x10EE
// Default AXI Bridge ID, we use this as the device ID
// 9031 for x1
// 903f for x16
#define DEVICE_ID 0x9031

#define DRIVER_NAME "maple-board-riscv"
#define BOARD_NAME "vcu1525"
#define NUM_BARS 7  //we use up to BAR4
#define CMA_SIZE 64L

#define BASEADDR 0x0
#define CONF 0x2
#define SLOT 0x4
#define DDR_DMA 0x6
#define CDMA 0x8


int fpga_open(struct inode *inode, struct file *file);
int fpga_close(struct inode *inode, struct file *file);
ssize_t fpga_read(struct file *file, char __user *buf, size_t count, loff_t *pos);
ssize_t fpga_write(struct file *file, const char __user *buf, size_t count, loff_t *pos);

static const size_t BUFFER_SIZE = PAGE_SIZE;

//Keep track of bits and bobs that we need for the driver
struct DevInfo_t {
  /* the kernel pci device data structure */
  struct pci_dev *pciDev;

  /* upstream root node */
  struct pci_dev *upstream;

  /* kernel's virtual addr. for the mapped BARs */
  void * __iomem bar[NUM_BARS];
  /* phys addr maybe? */
  unsigned long bar_phys[NUM_BARS];

  /* length of each memory region. Used for error checking. */
  size_t barLengths[NUM_BARS];

  /* temporary buffer. If allocated, will be BUFFER_SIZE. */
  char *buffer;

  /* Mutex for this device. */
  struct semaphore sem;

  /* PID of process that called open() */
  int userPID;

  /* character device */
  dev_t cdevNum;
  struct cdev cdev;
  struct class *myClass;
  struct device *device;

  // the buffer used for initializing the program
  // not actually dma but just contiguous physical mem
  // dma_mmap_coherent(dev, vma, virt, phys, size);
  void* dmaBuffer;
  dma_addr_t dmaPhys;
  dma_addr_t dmaPhysRound;

  // write the buffer into the axi bridge config
  // 0x208 BAR0 U
  // 0x20C BAR0 L
};
