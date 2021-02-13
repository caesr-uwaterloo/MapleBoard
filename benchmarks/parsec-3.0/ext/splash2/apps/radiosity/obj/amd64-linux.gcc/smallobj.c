
/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/

/************************************************************************
 *
 *	Class methods for small simple objects.
 *
 *
 *************************************************************************/
  
#include <stdio.h>
  

#include <pthread.h>
#include <sys/time.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
extern pthread_t PThreadTable[];
;
  

/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/


/*  This file contains many constant definitions that control the execution 
of the program, as well as lobal data structure declarations */

#ifndef _RADIOSITY_H
#define _RADIOSITY_H

#include <math.h>

/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/

/**************************************************************
*
*       Definitions relevant to parallel processing
*
***************************************************************/

#ifndef _PARALLEL_H
#define _PARALLEL_H



/***************************************************************************
*
*    Shared lock variable
*
*    Some machines provide only a limited number of lock variables. This
*    data structure allows sharing of these lock variables.
*    The shared locks are divided into 2 segments so that different types of
*    objects are given different locks. 
*
****************************************************************************/

typedef struct
{
    pthread_mutex_t (lock);
} Shared_Lock ;

#define SHARED_LOCK_SEG_SIZE (MAX_SHARED_LOCK / 2)

#define SHARED_LOCK_SEG0 (0)
#define SHARED_LOCK_SEG1 (1)
#define SHARED_LOCK_SEGANY (2)

extern void init_sharedlock() ;
extern Shared_Lock *get_sharedlock() ;

/****************************************************************************
*
*    Memory Consistency Model of the machine
*
*    Some macro changes its behavior based on the memory consistency model
*
*
*****************************************************************************/

/* Set one(1) to the model used in the machine.  Set only one of these
at a time */

#define MEM_CONSISTENCY_RELEASE    (0)
#define MEM_CONSISTENCY_WEAK       (0)
#define MEM_CONSISTENCY_PROCESSOR  (1)

#endif



/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/


#ifndef _PATCH_H
#define _PATCH_H


/************************************************************************
*
*     Constants
*
*************************************************************************/

#define F_COPLANAR  (5.0e-2)     /* H(P) < F_COPLANAR then P is on the plane */
#define N_VISIBILITY_TEST_RAYS  (10)	/* number of "random", "magic" rays fired 
between patches to test visibility */

#define FF_GEOMETRY_ERROR (1.0)		/* FF relative error due to Fdf approx
and cosine approx of angle */
#define FF_GEOMETRY_VARIANCE (1.0)	/* FF relative varance with in elem */
#define FF_VISIBILITY_ERROR (1.0 / N_VISIBILITY_TEST_RAYS)



/************************************************************************
*
*     Intersection code
*
*************************************************************************/

#define POINT_POSITIVE_SIDE   (1)
#define POINT_NEGATIVE_SIDE   (2)
#define POINT_ON_PLANE        (0)

#define P1_POSITIVE    (1)
#define P1_NEGATIVE    (2)
#define P2_POSITIVE    (4)
#define P2_NEGATIVE    (8)
#define P3_POSITIVE    (16)
#define P3_NEGATIVE    (32)
#define ANY_POSITIVE   (P1_POSITIVE | P2_POSITIVE | P3_POSITIVE)
#define ANY_NEGATIVE   (P1_NEGATIVE | P2_NEGATIVE | P3_NEGATIVE)
#define POSITIVE_SIDE(code) (((code) & ANY_NEGATIVE) == 0)
#define NEGATIVE_SIDE(code) (((code) & ANY_POSITIVE) == 0)
#define INTERSECTING(code)  (   ((code) & ANY_NEGATIVE) \
&& ((code) & ANY_POSITIVE) )
#define P1_CODE(code)  (code & 3)
#define P2_CODE(code)  ((code >> 2) & 3)
#define P3_CODE(code)  ((code >> 4) & 3)

/************************************************************************
*
*     Visibility Testing
*
*************************************************************************/

#define      VISIBILITY_UNDEF      ((float)-1.0)
#define      PATCH_CACHE_SIZE      (2)        /* The first two cache entries
covers about 95% of the total cache hits, so using
more doesn't help too much. */

extern float visibility() ;
extern void  compute_visibility_values() ;
extern void  visibility_task() ;

/************************************************************************
*
*     Refinement Advice
*
*************************************************************************/

#define _NO_INTERACTION          (1)
#define _NO_REFINEMENT_NECESSARY (2)
#define _REFINE_PATCH_1          (4)
#define _REFINE_PATCH_2          (8)
#define _NO_VISIBILITY_NECESSARY (16)

#define NO_INTERACTION(c)          ((c) & _NO_INTERACTION)
#define NO_REFINEMENT_NECESSARY(c) ((c) & _NO_REFINEMENT_NECESSARY)
#define REFINE_PATCH_1(c)          ((c) & _REFINE_PATCH_1)
#define REFINE_PATCH_2(c)          ((c) & _REFINE_PATCH_2)
#define NO_VISIBILITY_NECESSARY(c) ((c) & _NO_VISIBILITY_NECESSARY)


/************************************************************************
*
*     Vertex    -  3D coordinate
*
*************************************************************************/

typedef struct {
    float x, y, z ;
} Vertex ;


extern float vector_length() ;
extern float distance() ;
extern float normalize_vector() ;
extern float inner_product() ;
extern void cross_product() ;
extern float plane_normal() ;
extern void center_point(), four_center_points() ;
extern void print_point() ;


/************************************************************************
*
*     Color (R,G,B)
*
*************************************************************************/

typedef struct {
    float r, g, b ;
} Rgb ;



extern void print_rgb() ;



/************************************************************************
*
*     Element Vertex
*
*     ElementVertex represents a vertex of an element. A vertex structure
*     is shared by those elements which contain the vertex as part of their
*     vertex list.
*
*************************************************************************/

typedef struct _elemvertex {
    Vertex p ;			  /* Coordinate of the vertex */
    Rgb    col ;			  /* Color of the vertex */
    float  weight ;			  /* weight */
    Shared_Lock *ev_lock ;
} ElemVertex ;


#define N_ELEMVERTEX_ALLOCATE (16)
extern ElemVertex *get_elemvertex() ;
extern ElemVertex *create_elemvertex() ; /* Constructor */
extern void         init_elemvertex() ;	  /* Initialize free buffer */


/************************************************************************
*
*     Edge
*
*     Edge represents each edge of the element. Two adjacent elements
*     share the same edge. As an element is subdivided, the edge is also
*     subdivided. The edges form a binary tree, which can be viewed as a
*     projection of the element subdivision along an edge of the element.
*     In other words, the edge structure binds elements at the same height.
*     Note that the vertices may appear in reverse order in the edge structure
*     with respect to the order in the patch/element definition.
*
*************************************************************************/

typedef struct _edge {
    ElemVertex   *pa, *pb ;
    struct _edge *ea, *eb ;		  /* Edge (A-center) and (center-B) */
    Shared_Lock  *edge_lock ;	          /* Use segment0 */
} Edge ;


#define N_EDGE_ALLOCATE (16)
extern Edge *get_edge() ;
extern Edge *create_edge() ;		  /* Constructor */
extern void subdivide_edge() ;
extern void init_edge() ;		  /* Initialize free buffer */
extern void foreach_leaf_edge() ;

#define _LEAF_EDGE(e) ((e)->ea == 0)
#define EDGE_REVERSE(e,a,b) ((e)->pa == (b))


/************************************************************************
*
*     Planar equation
*
*     Plane equation (in implicit form) of the triangle patch.
*     A point P on the plane satisfies
*         (N.P) + C = 0
*     where N is the normal vector of the patch, C is a constant which
*     is the distance of the plane from the origin scaled by -|N|.
*
*************************************************************************/

typedef struct {
    Vertex  n ;		          /* Normal vector (normalized) */
    float  c ;			  /* Constant */
    /* Nx*x + Ny*y + Nz*z + C = 0 */
} PlaneEqu ;


extern float plane_equ() ;
extern float comp_plane_equ() ;
extern int point_intersection() ;
extern int patch_intersection() ;
extern void print_plane_equ() ;


/************************************************************************
*
*     Patch (also a node of the BSP tree)
*
*     The Patch represents a triangular patch (input polygon) of the given 
*     geometric model (i.e., room scene). The Patch contains 'per-patch' 
*     information such as the plane equation, area, and color. The Patch also 
*     serves as a node of the BSP tree which is used to test patch-patch 
*     visibility. The Patch points to the root level of the element quad-tree.
*     Geometrically speaking, the Patch and the root represent the same
*     triangle.
*     Although coordinates of the vertices are given by the Edge structure,
*     copies are stored in the Patch to allow fast access to the coordinates
*     during the visibility test.
*     For cost based task distribution another structure, Patch_Cost, is
*     also used. This structure is made separate from the Patch structure
*     since gathering cost statistics is a frequently read/write operation.
*     If it were in the Patch structure, updating a cost would result in
*     invalidation of the Patch structure and cause cache misses during
*     BSP traversal.
*
*************************************************************************/

struct _element ;

typedef struct _patch {
    ElemVertex *ev1, *ev2, *ev3 ;	  /* ElemVertecies of the patch */
    Edge    *e12, *e23, *e31 ;          /* Edges of the patch */
    Vertex   p1, p2, p3 ;		  /* Vertices of the patch */
    PlaneEqu plane_equ ;		  /* Plane equation H(x,y,z) */
    float    area ;			  /* Area of the patch */
    Rgb      color ;			  /* Diffuse color of the patch */
    /*       (reflectance) */
    Rgb      emittance ;	          /* Radiant emmitence */
    
    struct _patch  *bsp_positive ;	  /* BSP tree H(x,y,z) >= 0 */
    struct _patch  *bsp_negative ;	  /*          H(x,y,z) <  0 */
    struct _patch  *bsp_parent ;        /* BSP backpointer to the parent*/
    
    struct _element *el_root ;	  /* Root of the element tree */
    int      seq_no ;		          /* Patch sequence number */
} Patch ;

extern void foreach_patch_in_bsp(), foreach_depth_sorted_patch() ;
extern void define_patch() ;
extern void refine_newpatch() ;
extern Patch *get_patch() ;
extern void init_patchlist() ;
extern void print_patch() ;
extern void print_bsp_tree() ;


typedef struct {
    Patch    *patch ;
    Shared_Lock *cost_lock ;		  /* Cost variable lock */
    int      n_bsp_node ;	          /* Number of BSP nodes visited */
    int      n_total_inter ;	          /* Total number of interactions */
    int      cost_estimate ;            /* Cost estimate */
    int      cost_history[11] ;	  /* Cost history */
} Patch_Cost ;

/* Patch cost:
Visiting a node in BSP tree:  150 cyc (overall)
Gathering ray per interaction: 50 cyc (overall avg) */

#define PATCH_COST(p)          ((p)->n_bsp_node * 3 + (p)->n_total_inter)
#define PATCH_COST_ESTIMATE(p)  ((p)->cost_history[0] \
+ ((p)->cost_history[1] >> 1)\
+ ((p)->cost_history[2] >> 2) )


/************************************************************************
*
*     Element
*
*     The Element represents each node of the quad-tree generated by the 
*     hierarchical subdivision. The Element structure consists of:
*      - pointers to maintain the tree structure
*      - a linear list of interacting elements
*      - radiosity value of the element
*      - pointer to the vertex and edge data structures
*
*     To allow smooth radiosity interpolation across elements, an element
*     shares edges and vertices with adjacent elements.
*
*************************************************************************/

struct _interact ;

typedef struct _element {
    Shared_Lock *elem_lock ;	          /* Element lock variable (seg 1) */
    Patch *patch ;			  /* Original patch of the element */
    
    struct _element *parent ;		  /* Quad tree (parent)          */
    struct _element *center ;		  /*           (center triangle) */
    struct _element *top ;		  /*           (top)             */
    struct _element *left ;		  /*           (left)            */
    struct _element *right ;		  /*           (right)           */
    
    struct _interact *interactions ;	  /* Top of light interaction list */
    int  n_interactions ;		  /* Total # of interactions */
    struct _interact *vis_undef_inter ; /* Top of visibility undef list */
    int  n_vis_undef_inter ;		  /* # of interactions whose visibility
    is not yet calculated */
    Rgb  rad ;			  /* Radiosity of this element
    (new guess of B) */
    Rgb  rad_in ;			  /* Sum of anscestor's radiosity */
    Rgb  rad_subtree ;		  /* Area weighted sum of subtree's
    radiosity (includes this elem) */
    int  join_counter ;		  /* # of unfinished subprocesses */
    
    ElemVertex *ev1, *ev2, *ev3 ;	  /* Vertices of the element */
    Edge       *e12, *e23, *e31 ;	  /* Edges of the element */
    float area ;		          /* Area of the element */
} Element ;


extern void foreach_element_in_patch(), foreach_leaf_element_in_patch() ;
extern void ff_refine_elements() ;
extern void subdivide_element() ;
extern int  element_completely_invisible() ;
extern void process_rays() ;
extern Element *get_element() ;
extern void init_elemlist() ;
extern void print_element() ;

#define _LEAF_ELEMENT(e) ((e)->center == 0)

#if MEM_CONSISTENCY_PROCESSOR
#define LEAF_ELEMENT(e)  _LEAF_ELEMENT((e))
#endif

#if (MEM_CONSISTENCY_RELEASE || MEM_CONSISTENCY_WEAK)
extern int leaf_element() ;
#define LEAF_ELEMENT(e) (leaf_element((e)))
#endif


/************************************************************************
*
*     Interaction
*
*************************************************************************/

typedef struct _interact {
    struct _interact *next ;		  /* Next entry of the list */
    Element *destination ;	          /* Partner of the interaction */
    float   formfactor_out ;		  /* Form factor from this patch  */
    float   formfactor_err ;            /* Error of FF */
    float   area_ratio ;		  /* Area(this) / Area(dest) */
    float   visibility ;		  /* Visibility (0 - 1.0) */
} Interaction ;


extern void foreach_interaction_in_element() ;
extern void compute_interaction(), compute_formfactor() ;
extern void insert_interaction(), delete_interaction() ;
extern void insert_vis_undef_interaction(), delete_vis_undef_interaction() ;
extern void init_interactionlist() ;
extern Interaction *get_interaction() ;
extern void free_interaction() ;
extern void print_interaction() ;

#endif


/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/

/* Header file for model data structures and definitions */

#ifndef _MODEL_H
#define _MODEL_H


/************************************************************************
*
*     Constants
*
*************************************************************************/

#define MODEL_TRIANGLE  (0)
#define MODEL_RECTANGLE (1)
#define MODEL_NULL      (-1)

#define MODEL_TEST_DATA (0)
#define MODEL_ROOM_DATA (1)
#define MODEL_LARGEROOM_DATA (2)


/************************************************************************
*
*     Model descriptor
*
*************************************************************************/

/* General structure of the model descriptor */
typedef struct {
    Rgb   color ;			/* Diffuse color */
    Rgb   emittance ;		        /* Radiant emittance */
    Vertex _dummy[4] ;
} Model ;

/* Triangle */
typedef struct {
    Rgb   color ;			/* Diffuse color */
    Rgb   emittance ;		        /* Radiant emittance */
    Vertex p1, p2, p3 ;
} Model_Triangle ;

typedef Model_Triangle Model_Rectangle ;


typedef struct {
    int type ;
    Model model ;
} ModelDataBase ;



extern int model_selector ;
extern void process_model() ;
extern void init_modeling_tasks() ;

#endif




/*************************************************************************/
/*                                                                       */
/*  Copyright (c) 1994 Stanford University                               */
/*                                                                       */
/*  All rights reserved.                                                 */
/*                                                                       */
/*  Permission is given to use, copy, and modify this software for any   */
/*  non-commercial purpose as long as this copyright notice is not       */
/*  removed.  All other uses, including redistribution in whole or in    */
/*  part, are forbidden without prior written permission.                */
/*                                                                       */
/*  This software is provided with absolutely no warranty and no         */
/*  support.                                                             */
/*                                                                       */
/*************************************************************************/


#ifndef _TASK_H
#define _TASK_H


/************************************************************************
*
*     Constants
*
*************************************************************************/

#define PAGE_SIZE 4096   /* page size of system, used for padding to 
allow page placement of some logically 
per-process data structures */

/*** Task types ***/
#define TASK_MODELING      (1)
#define TASK_BSP           (2)
#define TASK_FF_REFINEMENT (4)
#define TASK_RAY           (8)
#define TASK_RAD_AVERAGE   (16)
#define TASK_VISIBILITY    (32)


/*** Controling parallelism ***/

#define MAX_TASKGET_RETRY (32)	    /* Max # of retry get_task() can make */
#define N_ALLOCATE_LOCAL_TASK (8)   /* get_task() and free_task() transfer
this # of task objects to/from the
global shared queue at a time */


/************************************************************************
*
*     Task Descriptors
*
*************************************************************************/

/* Decompose modeling object into patches (B-reps) */
typedef struct {
    int   type ;		     /* Object type */
    Model *model ;		     /* Object to be decomposed */
} Modeling_Task ;


/* Insert a new patch to the BSP tree */
typedef struct {
    Patch *patch ;                 /* Patch to be inserted */
    Patch *parent ;		     /* Parent node in the BSP tree */
} BSP_Task ;


/* Refine element interaction based on FF value or BF value */
typedef struct {
    Element *e1, *e2 ;	     /* Interacting elements */
    float   visibility ;           /* Visibility of parent */
    int level ;		     /* Path length from the root element */
} Refinement_Task ;


typedef struct {
    int  ray_type ;
    Element *e ;		     /* The element we are interested in */
} Ray_Task ;


typedef struct {
    Element *e ;		     /* The element we are interested in */
    Interaction *inter ;	     /* Top of interactions */
    int   n_inter ;		     /* Number of interactions */
    void  (*k)() ;		     /* Continuation */
} Visibility_Task ;

/* Radiosity averaging task */

#define RAD_AVERAGING_MODE (0)
#define RAD_NORMALIZING_MODE (1)

typedef struct {
    Element *e ;
    int level ;
    int mode ;
} RadAvg_Task ;



/************************************************************************
*
*     Task Definition
*
*************************************************************************/


typedef struct _task {
    int task_type ;
    struct _task *next ;
    union {
        Modeling_Task   model ;
        BSP_Task        bsp ;
        Refinement_Task ref ;
        Ray_Task        ray ;
        Visibility_Task vis ;
        RadAvg_Task     rad ;
    } task ;
} Task ;


typedef struct {
    char pad1[PAGE_SIZE];	 	/* padding to avoid false-sharing 
    and allow page-placement */	
    pthread_mutex_t (q_lock);
    Task  *top, *tail ;
    int   n_tasks ;
    pthread_mutex_t (f_lock);
    int   n_free ;
    Task  *free ;
    char pad2[PAGE_SIZE];	 	/* padding to avoid false-sharing 
    and allow page-placement */	
} Task_Queue ;


#define TASK_APPEND (0)
#define TASK_INSERT (1)


extern Task *get_task() ;
extern void free_task() ;
extern void enqueue_task() ;
extern Task *dequeue_task(), *dequeue_neighbor_task() ;
#define taskq_length(q)   (q->n_tasks)
#define taskq_top(q)      (q->top)
extern void print_task(), print_taskq() ;
extern int assign_taskq() ;
extern void init_taskq() ;
#define taskq_too_long(q)  ((q)->n_tasks > n_tasks_per_queue)

extern void process_tasks() ;
extern void create_modeling_task(), create_bsp_task() ;
extern void create_ff_refine_task() ;
extern void create_ray_task(), create_radavg_task() ;
extern void create_visibility_tasks() ;
extern void enqueue_ray_task() ;
extern void enqueue_radavg_task() ;
extern int  check_task_counter() ;


#endif


#include "glib.h"
#include "pslib.h"


/****************************************
*
*    Configuration Parameters
*
*****************************************/

/*************************************************************************
*
*    Task scheduling & Load balancing (1)
*       --- Assignment of the patches to the processors
*
*    This macro specifies how patches are assigned to the task queues (ie,
*    processors).
*    - PATCH_ASSIGNMENT_STATIC assigns the same set of patches to the same
*    queue repeatedly over iterations.
*    - PATCH_ASSIGNMENT_COSTBASED assigns patches to queues based on the
*    work associated with those patches in previous iterations, in order
*    to try to balance the initial workload assignment among processors
*    and hence reduce task stealing.
*
**************************************************************************/

#define PATCH_ASSIGNMENT_STATIC    (1)
#define PATCH_ASSIGNMENT_COSTBASED (3)

#if !defined(PATCH_ASSIGNMENT)
#define PATCH_ASSIGNMENT PATCH_ASSIGNMENT_STATIC
#endif


/****************************************
*
*    Constants
*
*****************************************/


#define F_ZERO  (1.0e-6)

#if defined(SIMULATOR)
#define MAX_PROCESSORS (128)	      /* Maximum number of processors
(i.e., processes) created */
#define MAX_TASKQUEUES (128)	      /* Maximum number of task queues */
#define MAX_TASKS    (32768)	      /* # of available task descriptors */
#define MAX_PATCHES  (1024)	      /* # of available patch objects */
#define MAX_ELEMENTS (80000)	      /* # of available element objects */
#define MAX_INTERACTIONS (640000)     /* # of available interaction objs */
#define MAX_ELEMVERTICES  (65536)     /* # of available ElemVertex objs */
#define MAX_EDGES  (65536)            /* # of available Edge objs */
#endif

#if defined(DASH)
#define MAX_PROCESSORS (64)	      /* Maximum number of processors
(i.e., processes) created */
#define MAX_TASKQUEUES (64)	      /* Maximum number of task queues */
#define MAX_TASKS    (32768)	      /* # of available task descriptors */
#define MAX_PATCHES  (1024)	      /* # of available patch objects */
#define MAX_ELEMENTS (80000)	      /* # of available element objects */
#define MAX_INTERACTIONS (640000)     /* # of available interaction objs */
#define MAX_ELEMVERTICES  (65536)     /* # of available ElemVertex objs */
#define MAX_EDGES  (65536)            /* # of available Edge objs */
#endif

#if defined(SGI_GL)
#define MAX_PROCESSORS (8)	      /* Maximum number of processors
(i.e., processes) created */
#define MAX_TASKQUEUES (8)	      /* Maximum number of task queues */
#define MAX_TASKS    (8192)	      /* # of available task descriptors */
#define MAX_PATCHES  (1024)	      /* # of available patch objects */
#define MAX_ELEMENTS (40000)	      /* # of available element objects */
#define MAX_INTERACTIONS (320000)     /* # of available interaction objs */
#define MAX_ELEMVERTICES  (16384)     /* # of available ElemVertex objs */
#define MAX_EDGES  (65536)            /* # of available Edge objs */
#endif

#if defined(SUN4)
#define MAX_PROCESSORS (1)	      /* Maximum number of processors
(i.e., processes) created */
#define MAX_TASKQUEUES (1)	      /* Maximum number of task queues */
#define MAX_TASKS    (1024)	      /* # of available task descriptors */
#define MAX_PATCHES  (1024)	      /* # of available patch objects */
#define MAX_ELEMENTS (20000)	      /* # of available element objects */
#define MAX_INTERACTIONS (160000)     /* # of available interaction objs */
#define MAX_ELEMVERTICES  (16384)     /* # of available ElemVertex objs */
#define MAX_EDGES  (32768)            /* # of available Edge objs */
#endif

#if (!defined(SIMULATOR) && !defined(DASH) && !defined(SGI_GL) && !defined(SUN4))
#define MAX_PROCESSORS (128)	      /* Maximum number of processors
(i.e., processes) created */
#define MAX_TASKQUEUES (128)	      /* Maximum number of task queues */
#define MAX_TASKS    (32768)	      /* # of available task descriptors */
#define MAX_PATCHES  (1024)	      /* # of available patch objects */
#define MAX_ELEMENTS (80000)	      /* # of available element objects */
#define MAX_INTERACTIONS (640000)     /* # of available interaction objs */
#define MAX_ELEMVERTICES  (65536)     /* # of available ElemVertex objs */
#define MAX_EDGES  (65536)            /* # of available Edge objs */
#endif

#define MAX_SHARED_LOCK (3900)	      /* Maximum locks allocated. Objects
share these locks */

#if defined(SGI_GL) || defined(DASH) || defined(SIMULATOR)
#define CLOCK_MAX_VAL (2048*1000000)  /* ANL macro clock max value */
#elif defined(SUN4)
#define CLOCK_MAX_VAL (65536*1000000)  /* ANL macro clock max value */
#else
#define CLOCK_MAX_VAL (2048*1000000)  /* ANL macro clock max value */
#endif



/****************************************
*
*    System defaults
*
*****************************************/

#define DEFAULT_N_PROCESSORS (1)
#define DEFAULT_N_TASKQUEUES (1)
#define DEFAULT_N_TASKS_PER_QUEUE (200)
/* Create new tasks if number of tasks currently
in the queue is less than this number */
#define DEFAULT_N_INTER_PARALLEL_BF_REFINEMENT (5)
/* If the number of interactions is greater than
or equal to this value, BF-refinement is
performed in parallel */
#define DEFAULT_N_VISIBILITY_PER_TASK (4)
/* Number of visibility computations per
visibility task */
#define DEFAULT_AREA_EPSILON (2000.0)
/* If element is smaller than this value,
no further subdivision takes place */
#define DEFAULT_ENERGY_EPSILON (0.005)
/* Terminate radiosity iteration if the
difference of total energy is less than this
value. */
#define DEFAULT_BFEPSILON (0.015)
/* BF refinement threshold level. If the estimated
error of BF (due to FF error and error due to
constant approximation within an element) is
larger than this value, then subdivide */

#define DFLT_VIEW_ROT_X (10.0)
#define DFLT_VIEW_ROT_Y (0.0)
#define DFLT_VIEW_DIST  (8000.0)
#define DFLT_VIEW_ZOOM  (1.0)


/****************************************
*
*    Display mode
*
*****************************************/

#define DISPLAY_FILLED   (0)
#define DISPLAY_SHADED   (1)
#define DISPLAY_EDGEONLY (2)

#define DISPLAY_ALL_INTERACTIONS  (0)
#define DISPLAY_HALF_INTERACTIONS (1)



/****************************************
*
*    Statistical Measure
*
*****************************************/

#define MAX_ITERATION_INFO (16)

struct _element ;

typedef struct
{
    int visibility_comp ;
    int ray_intersect_test ;
    int tasks_from_myq ;
    int tasks_from_otherq ;
    int process_tasks_wait ;
    struct _element *last_pr_task ;
} PerIterationInfo ;


typedef struct
{
    char pad1[PAGE_SIZE];	 	/* padding to avoid false-sharing 
    and allow page-placement */	
    int total_modeling_tasks ;
    int total_def_patch_tasks ;
    int total_ff_ref_tasks ;
    int total_ray_tasks ;
    int total_radavg_tasks ;
    int total_direct_radavg_tasks ;
    int total_interaction_comp ;
    int total_visibility_comp ;
    int partially_visible ;
    int total_ray_intersect_test ;
    int total_patch_cache_check ;
    int total_patch_cache_hit ;
    int patch_cache_hit[PATCH_CACHE_SIZE] ;
    PerIterationInfo per_iteration[ MAX_ITERATION_INFO ] ;
    char pad2[PAGE_SIZE];	 	/* padding to avoid false-sharing 
    and allow page-placement */	
} StatisticalInfo ;

extern void init_stat_info() ;

/****************************************
*
*    Shared data structure definition.
*
*****************************************/

typedef struct
{
    long int rad_start, rad_time, refine_time, wait_time, vertex_time;
} Timing;

typedef struct
{
    
    /* Task queue */
    /* ***** */ int index;
    /* ***** */	pthread_mutex_t (index_lock);
    Task_Queue task_queue[ MAX_TASKQUEUES ] ;
    Task task_buf[ MAX_TASKS ] ;
    
    /* BSP tree root */
    pthread_mutex_t (bsp_tree_lock);
    Patch *bsp_root ;
    
    /* Average radiosity value */
    pthread_mutex_t (avg_radiosity_lock);
    int   converged ;
    Rgb   prev_total_energy ;
    Rgb   total_energy ;
    float total_patch_area ;
    int   iteration_count ;
    
    /* Computation cost estimate */
    pthread_mutex_t (cost_sum_lock);
    int cost_sum ;
    int cost_estimate_sum ;
    Patch_Cost patch_cost[ MAX_PATCHES ] ;
    
    /* Barrier */
    
struct {
	pthread_mutex_t	mutex;
	pthread_cond_t	cv;
	unsigned long	counter;
	unsigned long	cycle;
} (barrier);

    
    /* Private varrier */
    int pbar_count ;
    pthread_mutex_t (pbar_lock);
    
    /* Task initializer counter */
    int task_counter ;
    pthread_mutex_t (task_counter_lock);
    
    /* Resource buffers */
    pthread_mutex_t (free_patch_lock);
    Patch *free_patch ;
    int   n_total_patches ;
    int   n_free_patches ;
    Patch patch_buf[ MAX_PATCHES ] ;
    
    pthread_mutex_t (free_element_lock);
    Element *free_element ;
    int     n_free_elements ;
    Element element_buf[ MAX_ELEMENTS ] ;
    
    pthread_mutex_t (free_interaction_lock);
    Interaction *free_interaction ;
    int         n_free_interactions ;
    Interaction interaction_buf[ MAX_INTERACTIONS ] ;
    
    pthread_mutex_t (free_elemvertex_lock);
    int         free_elemvertex ;
    ElemVertex  elemvertex_buf[ MAX_ELEMVERTICES ] ;
    
    pthread_mutex_t (free_edge_lock);
    int   free_edge ;
    Edge  edge_buf[ MAX_EDGES ] ;
    
    Shared_Lock sh_lock[ MAX_SHARED_LOCK ] ;
    
    StatisticalInfo stat_info[ MAX_PROCESSORS ] ;
    
} Global ;


/****************************************
*
*    Global variables
*
*****************************************/

extern Timing **timing ;
extern Global *global ;
extern int    n_processors ;
extern int    n_taskqueues ;
extern int    n_tasks_per_queue ;

extern int    N_inter_parallel_bf_refine ;
extern int    N_visibility_per_task ;
extern float  Area_epsilon ;
extern float  Energy_epsilon ;
extern float  BFepsilon ;

extern int batch_mode, verbose_mode ;
extern int taskqueue_id[] ; 

extern long time_rad_start, time_rad_end, time_process_start[] ;


/****************************************
*
*    Global function names & types
*
*****************************************/

extern void init_global() ;
extern void init_visibility_module() ;

extern void radiosity_averaging() ;
extern void setup_view() ;
extern void display_scene() ;
extern void display_patch(), display_patches_in_bsp_tree() ;
extern void display_element(), display_elements_in_patch() ;
extern void display_elements_in_bsp_tree() ;
extern void display_interactions_in_element() ;
extern void display_interactions_in_patch() ;
extern void display_interactions_in_bsp_tree() ;

extern void ps_display_scene() ;
extern void ps_display_patch(), ps_display_patches_in_bsp_tree() ;
extern void ps_display_element(), ps_display_elements_in_patch() ;
extern void ps_display_elements_in_bsp_tree() ;
extern void ps_display_interactions_in_element() ;
extern void ps_display_interactions_in_patch() ;
extern void ps_display_interactions_in_bsp_tree() ;

extern void print_statistics() ;
extern void print_running_time(), print_fork_time() ;
extern void print_usage() ;

extern void clear_radiosity(), clear_patch_radiosity() ;

extern void exit() ;


#endif


struct {
    char pad1[PAGE_SIZE];	 	/* padding to avoid false-sharing 
        and allow page-placement */	
    int          n_local_free_elemvertex    ;
    ElemVertex  *local_free_elemvertex      ;
    int    n_local_free_edge    ;
    Edge  *local_free_edge      ;
    int lock_alloc_counter  ;
    char pad2[PAGE_SIZE];	 	/* padding to avoid false-sharing 
        and allow page-placement */	
} sobj_struct[MAX_PROCESSORS];

  
/***************************************************************************
****************************************************************************
*
*    Methods for Vertex object
*
****************************************************************************
****************************************************************************/

/***************************************************************************
 *
 *    vector_length()
 *
 *    Comute length of a vector represented by Vertex
 *    length = | v | 
 *
 ****************************************************************************/
  
  float vector_length( v )
  
  Vertex *v ;
{
    double t0, t1, t2 ;
    
    t0 = v->x * v->x ;
    t1 = v->y * v->y ;
    t2 = v->z * v->z ;
    
    return( sqrt( t0 + t1 + t2 ) ) ;
}


/***************************************************************************
*
*    distance()
  *
  *    Comute distance of two points.
  *    dist = | P1 - P2 | 
  *
  ****************************************************************************/
  
  float distance( p1, p2 )
  
  Vertex *p1, *p2 ;
{
    Vertex v12 ;
    
    v12.x = p2->x - p1->x ;
    v12.y = p2->y - p1->y ;
    v12.z = p2->z - p1->z ;
    
    return( vector_length( &v12 ) ) ;
}

/***************************************************************************
*
*    normalize_vector()
  *
  *    Normalize vector represented by Vertex
  *    v1 <- normalized( v2 )
  *
  ****************************************************************************/
  
  float normalize_vector( v1, v2 )
  
  Vertex *v1, *v2 ;
{
    float t0 ;
    float length ;
    
    length = vector_length( v2 ) ;
    t0 = (float)1.0 / length ;
    
    v1->x = v2->x * t0 ;
    v1->y = v2->y * t0 ;
    v1->z = v2->z * t0 ;
    
    return( length ) ;
}


/**************************************************************************
*
*    inner_product()
  *
  *          (v1.v2) <- inner_product( v1, v2 )
  *
  ***************************************************************************/
  
  float inner_product( v1, v2 )
  
  Vertex *v1, *v2 ;
{
    float ip ;
    
    ip  = v1->x * v2->x ;
    ip += v1->y * v2->y ;
    ip += v1->z * v2->z ;
    
    return( ip ) ;
}


/**************************************************************************
*
*    cross_product()
  *
  *          Vc = V1 X V2
  *
  ***************************************************************************/
  
  void cross_product( vc, v1, v2 )
  
  Vertex *vc, *v1, *v2 ;
{
    vc->x = v1->y * v2->z  -  v1->z * v2->y ;
    vc->y = v1->z * v2->x  -  v1->x * v2->z ;
    vc->z = v1->x * v2->y  -  v1->y * v2->x ;
}


/**************************************************************************
*
*    plane_normal()
  *
  *          Vc = (P2-P1) X (P3-P1) /  |(P2-P1) X (P3-P1)|
  *
  ***************************************************************************/
  
  float plane_normal( vc, p1, p2, p3 )
  
  Vertex *vc, *p1, *p2, *p3 ;
{
    Vertex v1, v2 ;
    
    /* Compute vectors */
    v1.x = p2->x - p1->x ;
    v1.y = p2->y - p1->y ;
    v1.z = p2->z - p1->z ;
    
    v2.x = p3->x - p1->x ;
    v2.y = p3->y - p1->y ;
    v2.z = p3->z - p1->z ;
    
    /* Compute cross product and normalize */
    cross_product( vc, &v1, &v2 ) ;
    return( normalize_vector( vc, vc ) ) ;
}



/**************************************************************************
*
*    center_point()
  *
  *          P = (P1 + P2 + P3) / 3
  *
  ***************************************************************************/
  
  void center_point( p1, p2, p3, pc )
  
  Vertex *p1, *p2, *p3 ;		/* 3 vertices of a triangle */
  Vertex *pc ;			/* Center point (RETURNED) */
{
    /* Compute mid point of the element */
    
    pc->x = (p1->x + p2->x + p3->x) * (float)(1.0/3.0) ;
    pc->y = (p1->y + p2->y + p3->y) * (float)(1.0/3.0) ;
    pc->z = (p1->z + p2->z + p3->z) * (float)(1.0/3.0) ;
}



/**************************************************************************
*
*    four_center_points()
  *
  *          P = (P1 + P2 + P3) / 3
  *
  ***************************************************************************/
  
  void four_center_points( p1, p2, p3, pc, pc1, pc2, pc3 )
  
  Vertex *p1, *p2, *p3 ;		/* 3 vertices of a triangle */
  Vertex *pc ;			/* Center point (RETURNED) */
  Vertex *pc1, *pc2, *pc3 ;		/* Center points (RETURNED) */
{
    /* Compute mid point of the element */
    pc->x  = (p1->x + p2->x + p3->x) * (float)(1.0/3.0) ;
    pc->y  = (p1->y + p2->y + p3->y) * (float)(1.0/3.0) ;
    pc->z  = (p1->z + p2->z + p3->z) * (float)(1.0/3.0) ;
    
    pc1->x = (p1->x * 4 + p2->x + p3->x) * (float)(1.0/6.0) ;
    pc1->y = (p1->y * 4 + p2->y + p3->y) * (float)(1.0/6.0) ;
    pc1->z = (p1->z * 4 + p2->z + p3->z) * (float)(1.0/6.0) ;
    
    pc2->x = (p1->x + p2->x * 4 + p3->x) * (float)(1.0/6.0) ;
    pc2->y = (p1->y + p2->y * 4 + p3->y) * (float)(1.0/6.0) ;
    pc2->z = (p1->z + p2->z * 4 + p3->z) * (float)(1.0/6.0) ;
    
    pc3->x = (p1->x + p2->x + p3->x * 4) * (float)(1.0/6.0) ;
    pc3->y = (p1->y + p2->y + p3->y * 4) * (float)(1.0/6.0) ;
    pc3->z = (p1->z + p2->z + p3->z * 4) * (float)(1.0/6.0) ;
}


/***************************************************************************
*
*    print_point()
  *
  *    Print point information.
  *
  ****************************************************************************/
  
  void print_point( point, process_id )
  
  Vertex *point ;
  unsigned process_id;
{
    printf( "\tP(%.2f, %.2f, %.2f)\n", point->x, point->y, point->z ) ;
}




/***************************************************************************
****************************************************************************
*
*    Methods for Rgb object
*
****************************************************************************
****************************************************************************
****************************************************************************
*
*    print_rgb()
  *
  *    Print RGB information.
  *
  ****************************************************************************/
  
  void print_rgb( rgb, process_id )
  
  Rgb *rgb ;
  unsigned process_id;
{
    printf( "\tRGB(%.2f, %.2f, %.2f)\n", rgb->r, rgb->g, rgb->b ) ;
}




/***************************************************************************
****************************************************************************
*
*    Methods for ElementVertex
*
****************************************************************************
****************************************************************************/
/***************************************************************************
*
*    create_elemvertex
*
*    Given Vertex, create and return a new ElemVertex object.
*
****************************************************************************/

ElemVertex *create_elemvertex( p, process_id )
  
  Vertex *p ;
  unsigned process_id;
{
    ElemVertex *ev_new ;
    
    ev_new = get_elemvertex(process_id) ;
    ev_new->p = *p ;
    
    return( ev_new ) ;
}


/***************************************************************************
*
*    get_elemvertex
*
*    Returns an ElementVertex object
*
****************************************************************************/



ElemVertex *get_elemvertex(process_id)
{
    ElemVertex *ev ;
    
    if( sobj_struct[process_id].n_local_free_elemvertex == 0 )
        {
            {pthread_mutex_lock(&(global->free_elemvertex_lock));};
            if ( MAX_ELEMVERTICES - global->free_elemvertex
                < N_ELEMVERTEX_ALLOCATE )
                {
                    fprintf( stderr, "Fatal:Ran out of ElemVertex buffer\n" ) ;
                    {pthread_mutex_unlock(&(global->free_elemvertex_lock));};
                    exit(1) ;
                }
            sobj_struct[process_id].n_local_free_elemvertex = N_ELEMVERTEX_ALLOCATE ;
            sobj_struct[process_id].local_free_elemvertex
                = &global->elemvertex_buf[ global->free_elemvertex ] ;
            global->free_elemvertex += N_ELEMVERTEX_ALLOCATE ;
            {pthread_mutex_unlock(&(global->free_elemvertex_lock));};
        }
    
    ev = sobj_struct[process_id].local_free_elemvertex++ ;
    sobj_struct[process_id].n_local_free_elemvertex-- ;
    
    
    /* Initialize contents */
    ev->col.r  = 0.0 ;
    ev->col.g  = 0.0 ;
    ev->col.b  = 0.0 ;
    ev->weight = 0.0 ;
    
    return( ev ) ;
}


/***************************************************************************
*
*    init_elemvertex()
  *
  *    Initialize ElemVertex buffer.
  *    This routine must be called in single process state.
  *
  ****************************************************************************/
  
  
  void init_elemvertex(process_id)
  unsigned process_id;
{
    int ev_cnt ;
    
    /* Initialize global free list */
    {pthread_mutex_init(&(global->free_elemvertex_lock), NULL);};
    global->free_elemvertex = 0 ;
    
    /* Allocate locks */
    for( ev_cnt = 0 ; ev_cnt < MAX_ELEMVERTICES ; ev_cnt++ )
        global->elemvertex_buf[ ev_cnt ].ev_lock
            = get_sharedlock( SHARED_LOCK_SEGANY, process_id ) ;
    
    /* Initialize local free list */
    sobj_struct[process_id].n_local_free_elemvertex    = 0 ;
    sobj_struct[process_id].local_free_elemvertex      = 0 ;
}



/***************************************************************************
****************************************************************************
*
*    Methods for Edge
*
****************************************************************************
****************************************************************************/


/***************************************************************************
*
*    foreach_leaf_edge()
  *
  *    For each leaf edges of the binary edge tree, apply the specified
  *    function. Edges are traversed from A to B (i.e., from Pa of the root
                                                  *    to the Pb of the root) if 'reverse' is 0. Otherwise, it is traversed
                                                  *    from B to A.
                                                  *
                                                  ****************************************************************************/
  
  void foreach_leaf_edge( edge, reverse, func, arg1, arg2, process_id )
  
  Edge *edge ;		/* Root edge */
  int reverse ;		/* Reverse traversal  */
  void (*func)() ;		/* Function applied at the leaves */
  void* arg1;
  long  arg2 ;		/* Arguments */
  unsigned process_id;
{
    Edge *first, *second ;
    
    if( edge == 0 )
        return ;
    
    if( (edge->ea == 0) && (edge->eb == 0) )
        func( edge, reverse, arg1, arg2, process_id ) ;
    else
        {
            if( reverse )
                {
                    first = edge->eb ;
                    second = edge->ea ;
                }
            else
                {
                    first = edge->ea ;
                    second = edge->eb ;
                }
            if( first )
                foreach_leaf_edge( first, reverse, func, arg1, arg2, process_id ) ;
            if( second )
                foreach_leaf_edge( second, reverse, func, arg1, arg2, process_id ) ;
        }
}


/***************************************************************************
*
*    create_edge()
  *
  *    Given two ElemVertices V1 and V2, create a new edge (V1,V2)
  *
  ****************************************************************************/
  
  Edge *create_edge( v1, v2, process_id )
  
  ElemVertex *v1, *v2 ;
{
    Edge *enew ;
    
    enew = get_edge(process_id) ;
    enew->pa = v1 ;
    enew->pb = v2 ;
    return( enew ) ;
}


/***************************************************************************
*
*    subdivide_edge()
  *
  *    Create child edges. If they already exist, do nothing.
  *
  ****************************************************************************/
  
  void subdivide_edge( e, a_ratio, process_id )
  
  Edge *e ;		     /* Parent edge */
  float a_ratio ;	     /* ratio*Pa + (1-ratio)*Pb */
  unsigned process_id;
{
    Edge *enew, *e_am ;
    ElemVertex *ev_middle ;
    float b_ratio ;
    
    /* Lock the element before checking the value */
    {pthread_mutex_lock(&(e->edge_lock->lock));};
    
    /* Check if the element already has children */
    if( ! _LEAF_EDGE(e) )
        {
            {pthread_mutex_unlock(&(e->edge_lock->lock));};
            return ;
        }
    
    /* Create the subdivision point */
    b_ratio = (float)1.0 - a_ratio ;
    ev_middle = get_elemvertex(process_id) ;
    ev_middle->p.x = a_ratio * e->pa->p.x + b_ratio * e->pb->p.x ;
    ev_middle->p.y = a_ratio * e->pa->p.y + b_ratio * e->pb->p.y ;
    ev_middle->p.z = a_ratio * e->pa->p.z + b_ratio * e->pb->p.z ;
    
    /* (1) Create edge(A-middle) */
    enew = get_edge(process_id) ;
    e_am = enew ;
    enew->pa = e->pa ;
    enew->pb = ev_middle ;
    
    /* (2) Create edge(middle-B) */
    enew = get_edge(process_id) ;
    enew->pa = ev_middle ;
    enew->pb = e->pb ;
    e->eb = enew ;
    
    /* Finally, set e->ea */
    e->ea = e_am ;
    
    /* Unlock the element */
    {pthread_mutex_unlock(&(e->edge_lock->lock));};
}


/***************************************************************************
*
*    get_edge
*
*    Returns an Edge object
*
****************************************************************************/



Edge *get_edge(process_id)
  unsigned process_id;
{
    Edge *edge ;
    
    if( sobj_struct[process_id].n_local_free_edge == 0 )
        {
            {pthread_mutex_lock(&(global->free_edge_lock));};
            if ( MAX_EDGES - global->free_edge < N_EDGE_ALLOCATE )
                {
                    fprintf( stderr, "Fatal:Ran out of Edge buffer\n" ) ;
                    {pthread_mutex_unlock(&(global->free_edge_lock));};
                    exit(1) ;
                }
            sobj_struct[process_id].n_local_free_edge = N_EDGE_ALLOCATE ;
            sobj_struct[process_id].local_free_edge
                = &global->edge_buf[ global->free_edge ] ;
            global->free_edge += N_EDGE_ALLOCATE ;
            {pthread_mutex_unlock(&(global->free_edge_lock));};
        }
    
    edge = sobj_struct[process_id].local_free_edge++ ;
    sobj_struct[process_id].n_local_free_edge-- ;
    
    
    /* Initialize contents */
    edge->pa = 0 ;
    edge->pb = 0 ;
    edge->ea = 0 ;
    edge->eb = 0 ;
    
    return( edge ) ;
}


/***************************************************************************
*
*    init_edge()
  *
  *    Initialize Edge buffer.
  *    This routine must be called in single process state.
  *
  ****************************************************************************/
  
  
  void init_edge(process_id)
  unsigned process_id;
{
    int edge_cnt ;
    
    /* Initialize global free list */
    {pthread_mutex_init(&(global->free_edge_lock), NULL);};
    global->free_edge = 0 ;
    
    /* Allocate locks */
    for( edge_cnt = 0 ; edge_cnt < MAX_EDGES ; edge_cnt++ )
        global->edge_buf[ edge_cnt ].edge_lock
            = get_sharedlock( SHARED_LOCK_SEG0, process_id ) ;
    
    /* Initialize local free list */
    sobj_struct[process_id].n_local_free_edge    = 0 ;
    sobj_struct[process_id].local_free_edge      = 0 ;
}



/***************************************************************************
****************************************************************************
*
*    Methods for Shared_Lock
*
*    Some machines provide a limited number of lock variables due to hardware
*    constraints etc. This package controls the sharing of this limited number
*    of locks among objects.
*
****************************************************************************
****************************************************************************/
/***************************************************************************
*
*    init_sharedlock()
  *
  *    Initialize shared lock.
  *
  ****************************************************************************/
  
  
  void init_sharedlock(process_id)
  unsigned process_id;
{
    int i ;
    
    for( i = 0 ; i < MAX_SHARED_LOCK ; i++ )
        {
            {pthread_mutex_init(&(global->sh_lock[i].lock), NULL);};
        }
    
    sobj_struct[process_id].lock_alloc_counter = 0 ;
}


/***************************************************************************
*
*    get_sharedlock()
  *
  *    Return a shared lock variable. If SHARED_LOCK_SEG[01] is specified,
  *    the lock is selected from the specified segment. If SHARED_LOCK_SEGANY
  *    is specified, the lock is picked up from arbitrary segment.
  *
  ****************************************************************************/
  
  Shared_Lock *get_sharedlock( segment, process_id )
  
  int segment ;
  unsigned process_id;
{
    Shared_Lock *pshl ;
    int effective_lock_ctr ;
    
    /* Compute effective lock allocation counter value */
    switch( segment )
        {
        case SHARED_LOCK_SEG0:
            effective_lock_ctr = sobj_struct[process_id].lock_alloc_counter % SHARED_LOCK_SEG_SIZE ;
            break ;
        case SHARED_LOCK_SEG1:
            effective_lock_ctr = sobj_struct[process_id].lock_alloc_counter % SHARED_LOCK_SEG_SIZE
                + SHARED_LOCK_SEG_SIZE ;
            break ;
        default:
            effective_lock_ctr = sobj_struct[process_id].lock_alloc_counter ;
        }
    
    
    /* Get pointer to the lock */
    pshl = &global->sh_lock[ effective_lock_ctr ] ;
    
    /* Update the lock counter */
    sobj_struct[process_id].lock_alloc_counter++ ;
    if( sobj_struct[process_id].lock_alloc_counter >= MAX_SHARED_LOCK )
        sobj_struct[process_id].lock_alloc_counter = 0 ;
    
    return( pshl ) ;
}

