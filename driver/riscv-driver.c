#include "riscv-driver.h"


MODULE_LICENSE("GPL");
MODULE_AUTHOR("Zhuanhao Wu (z284wu@uwaterloo.ca)");
MODULE_DESCRIPTION("Driver for MapleBoard Shell on VCU1525.");

static int probe(struct pci_dev *dev, const struct pci_device_id *id);
static void remove(struct pci_dev *dev);


int fpga_mmap (struct file *, struct vm_area_struct *);

//Fill in kernel structures with a list of ids this driver can handle
static struct pci_device_id idTable[] = {
	{ PCI_DEVICE(VENDOR_ID, DEVICE_ID) },
	{ 0, },
};


MODULE_DEVICE_TABLE(pci, idTable);

static struct pci_driver fpgaDriver = {
	.name = DRIVER_NAME,
	.id_table = idTable,
	.probe = probe,
	.remove = remove,
};
struct file_operations fileOps = {
	.owner =    THIS_MODULE,
	.read =     fpga_read,
	.write =    fpga_write,
	.open =     fpga_open,
	.release =  fpga_close,
  .mmap    =  fpga_mmap,
};

int fpga_open(struct inode *inode, struct file *filePtr) {
	//Get a handle to our devInfo and store it in the file handle

  struct DevInfo_t * devInfo = 0;
	printk(KERN_INFO "[MapleBoard] fpga_open: Entering function.\n");
  
	devInfo = container_of(inode->i_cdev, struct DevInfo_t, cdev);

	if (down_interruptible(&devInfo->sem)) {
		printk(KERN_WARNING "[BBN FPGA] fpga_open: Unable to get semaphore!\n");
		return -1;
	}

  filePtr->private_data = devInfo;

  devInfo->userPID = current->pid;

	up(&devInfo->sem);


	if (down_interruptible(&devInfo->sem)) {
		printk(KERN_WARNING "[BBN FPGA] fpga_open: Unable to get semaphore!\n");
		return -1;
	}

	up(&devInfo->sem);

	printk(KERN_INFO "[MapleBoard] fpga_open: Leaving function.\n");

	return 0;
}
int fpga_close(struct inode *inode, struct file *filePtr){

	printk(KERN_INFO "[MapleBoard] fpga_close: Entering function.\n");
	printk(KERN_INFO "[MapleBoard] fpga_close: Leaving function.\n");
	return 0;
}
ssize_t fpga_read(struct file *filePtr, char __user *buf, size_t count, loff_t *pos){
	return 0;
}
ssize_t fpga_write(struct file *filePtr, const char __user *buf, size_t count, loff_t *pos){

  struct DevInfo_t * devInfo;
  printk(KERN_INFO "[MapleBoard] entering write, count: %lx\n", count);
  devInfo = (struct DevInfo_t *) filePtr->private_data;
  if(copy_from_user(devInfo->dmaBuffer, buf, count)) {
    printk(KERN_WARNING "[MapleBoard] cannot write to cma buffer!!\n");
    return -EFAULT;
  }
  return count;
}



static int setup_chrdev(struct DevInfo_t *devInfo){
	/*
	Setup the /dev/deviceName to allow user programs to read/write to the driver.
	*/

	int devMinor = 0;
	int devMajor = 0;
	int devNum = -1;
  int result = 0;
  printk(KERN_INFO "[MapleBoard] Setting up drv\n");


	result = alloc_chrdev_region(&devInfo->cdevNum, devMinor, 1 /* one device*/, BOARD_NAME);
	if (result < 0) {
		printk(KERN_WARNING "Can't get major ID\n");
		return -1;
	}
	devMajor = MAJOR(devInfo->cdevNum);
	devNum = MKDEV(devMajor, devMinor);

	//Initialize and fill out the char device structure
	cdev_init(&devInfo->cdev, &fileOps);
	devInfo->cdev.owner = THIS_MODULE;
	devInfo->cdev.ops = &fileOps;
	result = cdev_add(&devInfo->cdev, devNum, 1 /* one device */);
	if (result) {
		printk(KERN_NOTICE "Error %d adding char device for BBN FPGA driver with major/minor %d / %d", result, devMajor, devMinor);
		return -1;
	}

	return 0;
}


static int map_bars(struct DevInfo_t* devInfo) {
  int ct = 0;
  unsigned long barStart, barEnd, barLength;
  for(ct = 0; ct < NUM_BARS; ct += 1) {
		printk(KERN_INFO "[BBN FPGA] Trying to map BAR #%d of %d.\n", ct, NUM_BARS);
    barStart = pci_resource_start(devInfo->pciDev, ct);
    barEnd = pci_resource_end(devInfo->pciDev, ct);
    barLength = barEnd - barStart + 1;
    devInfo -> barLengths[ct] = barLength;
    if(!barStart || !barEnd) {
      printk(KERN_INFO "[MapleBoard] Empty BAR #%d.\n", ct);
      continue;
    }
    if(barLength < 1) {
      printk(KERN_WARNING "[MapleBoard] BAR #%d length is less than 1B.\n", ct);
      continue;
    }

    devInfo -> bar[ct] = pci_iomap(devInfo->pciDev, ct, barLength);
    devInfo -> bar_phys[ct] = barStart;
    if(ct == 2) {
      // *(unsigned long*)devInfo->bar[ct] = 0xdeadbeef;
    } else if(ct == 0) { // S_AXI_Lite of the XDMA
      // printk(KERN_INFO "writing to axi lite interface: %llx U: %llx L: %llx", devInfo->dmaPhysRound, devInfo->dmaPhysRound >> 32, devInfo->dmaPhysRound & 0xFFFFFFFFL);
      // *(u64*)(devInfo->bar[ct] + AXIBAR0_U) = ((devInfo -> dmaPhys & 0xFFFFFFFFL) << 32) | (devInfo -> dmaPhys >> 32);
       // *(u64*)(devInfo->bar[ct] + AXIBAR0_U) = devInfo->dmaPhys;
       //

     // read back
     u32 hi = *(u32*)(devInfo->bar[ct] + AXIBAR0_U);
     u32 lo = *(u32*)(devInfo->bar[ct] + AXIBAR0_L);
     u64 base = ((u64)hi << 32) | lo;
     printk(KERN_INFO "original it was... %llx", base);
     *(u32*)(devInfo->bar[ct] + AXIBAR0_U) = devInfo->dmaPhysRound >> 32;
     *(u32*)(devInfo->bar[ct] + AXIBAR0_L) = devInfo->dmaPhysRound & 0xFFFFFFFFL;

     // read back
     hi = *(u32*)(devInfo->bar[ct] + AXIBAR0_U);
     lo = *(u32*)(devInfo->bar[ct] + AXIBAR0_L);
     base = ((u64)hi << 32) | lo;
     printk(KERN_INFO "read back from register: %llx", base);
     if(base != devInfo->dmaPhysRound) {
       printk(KERN_WARNING "Cannot write to the AXI Bridge Interface");
       return -1;
     } else {
       printk(KERN_INFO "[MapleBoard] The information is correct");
     }
      // *(u64*)(devInfo->bar[0] + 0x100000) = 0x1111111111111111L;
    }
    if(!devInfo->bar[ct]) {
      printk(KERN_WARNING "[MapleBoard] Could not map BAR #%d. \n", ct);
      return -1;
    }

		printk(KERN_INFO "[MapleBoard] BAR #%d mapped at 0x%p with length %lu (BAR start: %lx).\n", ct, devInfo->bar[ct], barLength, barStart);
  }
  return 0;
}

static int unmap_bars(struct DevInfo_t* devInfo) {
  int ct = 0;
	for (ct = 0; ct < NUM_BARS; ct+=1) {
		if (devInfo->bar[ct]) {
			pci_iounmap(devInfo->pciDev, devInfo->bar[ct]);
			devInfo->bar[ct] = NULL;
		}
	}
	return 0;
}

static int probe(struct pci_dev *dev, const struct pci_device_id *id){
  dma_addr_t dma_phys;
  void* virt;
  struct DevInfo_t *devInfo = 0;
  printk(KERN_INFO "[MapleBoard] Entered driver probe function.\n");
  printk(KERN_INFO "[MapleBoard] vendor = 0x%x, device = 0x%x \n", dev->vendor, dev->device);


  devInfo = kzalloc(sizeof(struct DevInfo_t), GFP_KERNEL);
  if(!devInfo) {
    printk(KERN_WARNING "Couldn't allocate memory for device info!\n");
    return -1;
  }

  // and allocate buffer
  // 512 MB
  // hopefully not too large
  // 512 Mega
  dma_set_coherent_mask(&dev-> dev, DMA_ATTR_FORCE_CONTIGUOUS);
  virt = dma_alloc_coherent(&dev->dev,  (CMA_SIZE + 256) * 1024 * 1024, &dma_phys, GFP_KERNEL);
  if(virt == 0) {
    printk(KERN_WARNING "[MapleBoard] Cannot allocate DMA Buffer");
    return -1;
  }
  printk(KERN_WARNING "Allocated CMA memory: VIRT: %p, PHYS: %llx (at 0x2000 - %x)", virt, dma_phys, *(u32*)(virt + 0x2000));

  devInfo->dmaBuffer = virt;
  devInfo->dmaPhys = dma_phys;
  devInfo->dmaPhysRound = round_up(dma_phys, 1L << 28);
  printk(KERN_WARNING "Allocated CMA memory: RoundUp: %llx, (RoundEnd: %llx, REAL END: %llx)", 
      devInfo->dmaPhysRound, 
      devInfo->dmaPhysRound + CMA_SIZE * 1024 * 1024, 
      devInfo->dmaPhys + (CMA_SIZE + 256)*1024*1024);

  devInfo->pciDev = dev;
  //Save the device info itself into the pci driver
  dev_set_drvdata(&dev->dev, (void*) devInfo);

  //Setup the char device
  setup_chrdev(devInfo);

  devInfo->userPID = -1;
  devInfo->buffer = kmalloc (BUFFER_SIZE * sizeof(char), GFP_KERNEL);

  if (pci_enable_device(dev)){
    printk(KERN_WARNING "[BBN FPGA] pci_enable_device failed!\n");
    return -1;
  }

  pci_set_master(dev);
  pci_request_regions(dev, DRIVER_NAME);

  if(map_bars(devInfo)) {
    printk(KERN_WARNING "[MapleBoard] unable to map bar!\n");
    return -1;
  }
  

  // printk(KERN_INFO "[MapleBoard] Allocated DMA Buffer: Kernel Virt %p  Phys %llx", virt, dma_phys);


  sema_init(&devInfo->sem, 1);

  return 0;
}

static void remove(struct pci_dev *dev) {
  struct DevInfo_t *devInfo = 0;

	printk(KERN_INFO "[MapleBoard] Entered VCU1525 driver remove function.\n");

	devInfo = (struct DevInfo_t*) dev_get_drvdata(&dev->dev);
	if (devInfo == 0) {
		printk(KERN_WARNING "[MapleBoard] remove: devInfo is 0");
		return;
	}

	//Clean up the char device
	cdev_del(&devInfo->cdev);
	unregister_chrdev_region(devInfo->cdevNum, 1);

	//Release memory
	unmap_bars(devInfo);

	//TODO: does order matter here?
	pci_release_regions(dev);
	pci_disable_device(dev);

  // and free dma buffers
  dma_free_coherent(&dev->dev,  (CMA_SIZE + 256) * 1024 * 1024, devInfo->dmaBuffer, devInfo->dmaPhys);

  printk(KERN_INFO "[MapleBoard] Freed DMA Buffer.\n");

	kfree(devInfo->buffer);
	kfree(devInfo);
}

int fpga_mmap (struct file *filp, struct vm_area_struct *vma) {
  // we only map as VM_IO, other option is not guaranteed to work
  
  struct DevInfo_t * devInfo;
  unsigned long barid;
  // struct page* pg;
  // unsigned long pfn, phys, offset;
  unsigned long sz, offset;
  int res;
  // unsigned long vm_len, pages, start, len;

  printk(KERN_INFO "[MapleBoard] entering mmap\n");
  printk(KERN_INFO "[MapleBoard] mapping to %lx\n", vma->vm_start);

  devInfo = (struct DevInfo_t *) filp->private_data;

  offset = vma->vm_pgoff << PAGE_SHIFT;
  barid = offset;
  barid /= 0x1000; // <-- barid
  sz = vma->vm_end - vma->vm_start;
  printk(KERN_INFO "[MapleBoard] BARID: %lu\n", barid);
  if(barid != 1 && barid != 2 && barid !=4 && barid != 6 && barid != CDMA && barid != 10) {
    printk(KERN_WARNING "[MapleBoard] Unsupported BAR ID, should be 0 (use 1 to by pass mmap argument check), 2, 4\n");
    return -EAGAIN;
  }
  if(barid == 1 || barid == 2 || barid == SLOT || barid == CDMA || barid == 10) {
    if(barid == 1) barid = 0;
    vma->vm_flags |= (VM_IO | VM_LOCKED);
    // pg = virt_to_page(devInfo->bar[barid]);
    // phys = virt_to_phys(devInfo->bar[barid]);
    // pfn = page_to_pfn(pg);
    // printk(KERN_INFO "[MapleBoard] PFN: %lx PHY: %lx\n", pfn, phys);
    printk(KERN_INFO "[MapleBoard] Trying to map BAR %ld into user space...at: %lx\n", barid, vma->vm_start);

    printk(KERN_INFO "[MapleBoard] pgoff = %lx", vma->vm_pgoff);
    vma->vm_pgoff = 0;

    if(barid == 0) {
      // For configuration space
      printk(KERN_INFO "[MapleBoard] Trying to map XDMA Configuration space into user space (@offset 0x00000) (sz = %lx)", sz);
      if((res = vm_iomap_memory(vma, 
          devInfo->bar_phys[0] + 0x000000,
          sz
          ))) {
        return res;
      }
      return 0;

    } else if (barid == 2) {
      // For configuration space
      printk(KERN_INFO "[MapleBoard] Trying to map CONF space into user space (@offset 0x10000) (sz = %lx)", sz);
      if((res = vm_iomap_memory(vma, 
          devInfo->bar_phys[0] + 0x100000,
          sz
          ))) {
        return res;
      }
      return 0;
    } else if(barid == CDMA) {
      printk(KERN_INFO "[MapleBoard] Trying to map Central DMA Control Registers into user space");
      vma->vm_flags |= (VM_LOCKED | VM_IO);
      if((res = vm_iomap_memory(vma, 
          devInfo->bar_phys[0] + 0x200000,
          sz
          ))) {
        return res;
      }

    } else if(barid == SLOT) {
      printk(KERN_INFO "[MapleBoard] Trying to map SLOT space into user space (actual PCIe BAR 2), sz = %lu (MB)", sz / 1024 / 1024);
      vma->vm_flags |= (VM_LOCKED);
      if((res = vm_iomap_memory(vma, 
          devInfo->bar_phys[2] + vma->vm_start,
          sz
          ))) {
        return res;
      }
    }
    printk(KERN_INFO "[MapleBoard] mmap succ\n");
    return 0;
  } else if(barid == 10) {
    printk(KERN_INFO "[MapleBoard] Trying to map second SLOT space into user space (actual PCIe BAR 4), sz = %lu (MB)", sz / 1024 / 1024);
    printk(KERN_INFO "[MapleBoard] !!!");
    vma->vm_flags |= (VM_LOCKED);
    if((res = vm_iomap_memory(vma, 
            devInfo->bar_phys[4],
            sz
            ))) {
      return res;
    }

    
    printk(KERN_INFO "[MapleBoard] mmap succ\n");
    return 0;
  } else if(barid == DDR_DMA) {
    printk(KERN_INFO "[MapleBoard] Trying to map DMA buffer into user space... (sz: 0x%lx MB)", sz / 1024 / 1024);
    if(sz > (CMA_SIZE) * 1024 * 1024) {
      printk(KERN_WARNING "[MapleBoard] The memroy space to map is larger than the allocated buffer.\n");
      return -EAGAIN; 
    }
    printk(KERN_INFO "OFFSET: %lx", offset);
    vma->vm_flags |= (VM_LOCKED | VM_READ | VM_WRITE);
    vma->vm_pgoff = 0;
    printk(KERN_INFO "START: %lx", vma->vm_start);
    printk(KERN_INFO "PHYS: %llx END: %llx", devInfo->dmaPhysRound, devInfo->dmaPhysRound + sz);
    // vma->vm_page_prot = pgprot_noncached(vma->vm_page_prot);
    // NOTE: this way, it can only be used once
    if(remap_pfn_range(vma, 
          vma->vm_start, 
          // (devInfo->dmaPhysRound + vma->vm_start) >> PAGE_SHIFT, // a hack, otherwise, Xilinx IP won't work ...
          // this way, this mmap should only be called once, otherwise the same space will be mapped multiple times
          // (and the translation won't work)
          devInfo->dmaPhysRound >> PAGE_SHIFT,
          /* vma->vm_end - vma->vm_start, */
          sz,
          vma->vm_page_prot
          )) {
      return -EAGAIN;
    }
    return 0;
  } else {
    printk(KERN_WARNING "[MapleBoard] Invalid mmap operator (the barid/operation id is incorrect)\n");
    return -1;
  }
}

static int fpga_init(void) {
  printk(KERN_INFO "[MapleBoard] Loading VCU1525 Driver\n");
  return pci_register_driver(&fpgaDriver);
}
static void fpga_exit(void) {
  printk(KERN_INFO "[MapleBoard] Unloading VCU1525 Driver\n");
  pci_unregister_driver(&fpgaDriver);
}

module_init(fpga_init);
module_exit(fpga_exit);

