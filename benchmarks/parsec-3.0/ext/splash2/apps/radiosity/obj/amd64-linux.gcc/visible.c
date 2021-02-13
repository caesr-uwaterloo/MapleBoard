
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
 *	Visibility testing
 *
 *
 ***************************************************************/

#include	<stdio.h>
#include        <math.h>


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


#define VIS_RANGE_MARGIN (0.01)

#define		VISI_RAYS_MAX   (16)
#define         VISI_POOL_NO    (16)

#define FABS(x)  (((x) < 0)?-(x):(x))

typedef struct {
    float x, y, z;
} Ray;


float rand_ray1[VISI_RAYS_MAX][2], rand_ray2[VISI_RAYS_MAX][2] ;

struct v_struct {
    char pad1[PAGE_SIZE];	 	/* padding to avoid false-sharing 
                                   and allow page-placement */	
    Patch *v_src_patch, *v_dest_patch ;
    Vertex v_src_p1,   v_dest_p1 ;
    Vertex v_src_v12,  v_src_v13 ;
    Vertex v_dest_v12, v_dest_v13 ;
    
    int bsp_nodes_visited, total_bsp_nodes_visited ;
    Ray ray_pool[VISI_POOL_NO];
    Vertex point_pool[VISI_POOL_NO];
    int pool_dst_hits;	/* Number of rays that hit the destination  */
    Patch *patch_cache[PATCH_CACHE_SIZE] ;
    char pad2[PAGE_SIZE];	 	/* padding to avoid false-sharing 
                                   and allow page-placement */	
} vis_struct[MAX_PROCESSORS];


/*************************************************************
 *
 *	void init_visibility_module()
 *
 *       initialize the random test rays array.
 *
 *       Test ray parameters are precomputed as follows.
 *       (1) The triangles are divided into 16 small triangles.
 *       (2) Each small triangle shoots a ray to a small triangle on the
 *           destination. If N-rays are requested by get_test_ray(),
 *           N small triangles are chosen on the source and the destination
 *           and a ray is shot between N pairs of the small triangles.
 *       (3) Current scheme selects pairs of the small triangles in a static
 *           manner. The pairs are chosen such that rays are equally
 *           distributed.
 *	
 *************************************************************/

void init_visibility_module(process_id)
  unsigned process_id;
{
    void init_patch_cache() ;
    
#define TTICK (1.0/12.0)
    
    /* Three corner triangles. P(i) -- Q(i) */
    rand_ray1[0][0] = TTICK ;      rand_ray1[0][1] = TTICK ;
    rand_ray1[1][0] = TTICK ;      rand_ray1[1][1] = TTICK * 10 ;
    rand_ray1[2][0] = TTICK * 10 ; rand_ray1[2][1] = TTICK ;
    rand_ray2[0][0] = TTICK ;      rand_ray2[0][1] = TTICK ;
    rand_ray2[1][0] = TTICK ;      rand_ray2[1][1] = TTICK * 10 ;
    rand_ray2[2][0] = TTICK * 10 ; rand_ray2[2][1] = TTICK ;
    
    /* Three triangles adjacent to the corners. RotLeft P(i)--> Q(i+1) */
    rand_ray1[3][0] = TTICK * 2 ;  rand_ray1[3][1] = TTICK * 2 ;
    rand_ray1[4][0] = TTICK * 2 ;  rand_ray1[4][1] = TTICK * 8 ;
    rand_ray1[5][0] = TTICK * 8 ;  rand_ray1[5][1] = TTICK * 2 ;
    rand_ray2[4][0] = TTICK * 2 ;  rand_ray2[4][1] = TTICK * 2 ;
    rand_ray2[5][0] = TTICK * 2 ;  rand_ray2[5][1] = TTICK * 8 ;
    rand_ray2[3][0] = TTICK * 8 ;  rand_ray2[3][1] = TTICK * 2 ;
    
    /* Three triangles adjacent to the center. RotRight P(i)--> Q(i-1) */
    rand_ray1[6][0] = TTICK * 2 ;  rand_ray1[6][1] = TTICK * 5 ;
    rand_ray1[7][0] = TTICK * 5 ;  rand_ray1[7][1] = TTICK * 5 ;
    rand_ray1[8][0] = TTICK * 5 ;  rand_ray1[8][1] = TTICK * 2 ;
    rand_ray2[8][0] = TTICK * 2 ;  rand_ray2[8][1] = TTICK * 5 ;
    rand_ray2[6][0] = TTICK * 5 ;  rand_ray2[6][1] = TTICK * 5 ;
    rand_ray2[7][0] = TTICK * 5 ;  rand_ray2[7][1] = TTICK * 2 ;
    
    /* Center triangle. Straight P(i) --> Q(i) */
    rand_ray1[9][0] = TTICK * 4 ;  rand_ray1[9][1] = TTICK * 4 ;
    rand_ray2[9][0] = TTICK * 4 ;  rand_ray2[9][1] = TTICK * 4 ;
    
    /* Along the pelimeter. RotRight P(i)--> Q(i-1) */
    rand_ray1[10][0] = TTICK * 1 ;  rand_ray1[10][1] = TTICK * 7 ;
    rand_ray1[11][0] = TTICK * 5 ;  rand_ray1[11][1] = TTICK * 4 ;
    rand_ray1[12][0] = TTICK * 4 ;  rand_ray1[12][1] = TTICK * 1 ;
    rand_ray2[12][0] = TTICK * 1 ;  rand_ray2[12][1] = TTICK * 7 ;
    rand_ray2[10][0] = TTICK * 5 ;  rand_ray2[10][1] = TTICK * 4 ;
    rand_ray2[11][0] = TTICK * 4 ;  rand_ray2[11][1] = TTICK * 1 ;
    
    /* Along the pelimeter. RotLeft P(i)--> Q(i+1) */
    rand_ray1[13][0] = TTICK * 1 ;  rand_ray1[13][1] = TTICK * 4 ;
    rand_ray1[14][0] = TTICK * 4 ;  rand_ray1[14][1] = TTICK * 7 ;
    rand_ray1[15][0] = TTICK * 7 ;  rand_ray1[15][1] = TTICK * 1 ;
    rand_ray2[14][0] = TTICK * 1 ;  rand_ray2[14][1] = TTICK * 4 ;
    rand_ray2[15][0] = TTICK * 4 ;  rand_ray2[15][1] = TTICK * 7 ;
    rand_ray2[13][0] = TTICK * 7 ;  rand_ray2[13][1] = TTICK * 1 ;
    
    /* Initialize patch cache */
    init_patch_cache(process_id) ;
}


/*************************************************************
 *
 *	void get_test_ray(): get a randomized ray vector and start point.
 *
 *	Place: main loop.
 *
 *	Storage: must keep in the invidiual processor.
 *	
 *************************************************************/

void get_test_rays( p_src, v, no, process_id )
  
  Vertex *p_src;
  Ray *v;
  int no;
  unsigned process_id;
{
    int g_index, i ;
    Vertex p_dst ;
    float fv1, fv2 ;
    
    if( no > VISI_RAYS_MAX )
        no = VISI_RAYS_MAX ;
    
    for (i = 0, g_index = 0 ; i < no; i++, g_index++) {
        
        fv1 = rand_ray1[ g_index ][0] ;
        fv2 = rand_ray1[ g_index ][1] ;
        p_src->x = vis_struct[process_id].v_src_p1.x + vis_struct[process_id].v_src_v12.x * fv1 + vis_struct[process_id].v_src_v13.x * fv2 ;
        p_src->y = vis_struct[process_id].v_src_p1.y + vis_struct[process_id].v_src_v12.y * fv1 + vis_struct[process_id].v_src_v13.y * fv2 ;
        p_src->z = vis_struct[process_id].v_src_p1.z + vis_struct[process_id].v_src_v12.z * fv1 + vis_struct[process_id].v_src_v13.z * fv2 ;
        
        fv1 = rand_ray2[ g_index ][0] ;
        fv2 = rand_ray2[ g_index ][1] ;
        p_dst.x = vis_struct[process_id].v_dest_p1.x + vis_struct[process_id].v_dest_v12.x * fv1 + vis_struct[process_id].v_dest_v13.x * fv2 ;
        p_dst.y = vis_struct[process_id].v_dest_p1.y + vis_struct[process_id].v_dest_v12.y * fv1 + vis_struct[process_id].v_dest_v13.y * fv2 ;
        p_dst.z = vis_struct[process_id].v_dest_p1.z + vis_struct[process_id].v_dest_v12.z * fv1 + vis_struct[process_id].v_dest_v13.z * fv2 ;
        
        v->x = p_dst.x - p_src->x;
        v->y = p_dst.y - p_src->y;
        v->z = p_dst.z - p_src->z;
        
        p_src++;
        v++;
    }
}


/************************************************************************
 *
 *	int v_intersect(): check if the ray intersects with the polygon.
 *
 *************************************************************************/


int v_intersect( patch, p, ray, t, process_id )
  
  Patch *patch;
  Vertex *p;
  Ray *ray;
  float t ;
  unsigned process_id;
{
    float px, py, pz;
    float nx, ny, nz;
    float rx, ry, rz;
    float x, y, x1, x2, x3, y1, y2, y3;
    float a, b, c;
    int nc, sh, nsh;
    
    nx = patch->plane_equ.n.x;
    ny = patch->plane_equ.n.y;
    nz = patch->plane_equ.n.z;
    
    rx = ray->x;
    ry = ray->y;
    rz = ray->z;
    
    px = p->x;
    py = p->y;
    pz = p->z;
    
    
    a = FABS(nx); b = FABS(ny); c = FABS(nz);
    
    if (a > b) 
        if (a > c) {
            x  = py + t * ry; y = pz + t * rz;
            x1 = patch->p1.y; y1 = patch->p1.z;
            x2 = patch->p2.y; y2 = patch->p2.z;
            x3 = patch->p3.y; y3 = patch->p3.z;
        } else {
            x  = px + t * rx; y = py + t * ry;
            x1 = patch->p1.x; y1 = patch->p1.y;
            x2 = patch->p2.x; y2 = patch->p2.y;
            x3 = patch->p3.x; y3 = patch->p3.y;
        }
    else if (b > c) {
        x  = px + t * rx; y = pz + t * rz;
        x1 = patch->p1.x; y1 = patch->p1.z;
        x2 = patch->p2.x; y2 = patch->p2.z;
        x3 = patch->p3.x; y3 = patch->p3.z;
    } else {
        x  = px + t * rx; y = py + t * ry;
        x1 = patch->p1.x; y1 = patch->p1.y;
        x2 = patch->p2.x; y2 = patch->p2.y;
        x3 = patch->p3.x; y3 = patch->p3.y;
    }
    
    
    x1 -= x; y1 -= y;
    x2 -= x; y2 -= y;
    x3 -= x; y3 -= y;
    
    nc = 0;
    
    if (y1 >= 0.0)
        sh = 1;
    else
        sh = -1;
    
    if (y2 >= 0.0)
        nsh = 1;
    else 
        nsh = -1;
    
    if (sh != nsh) {
        if ((x1 >= 0.0) && (x2 >= 0.0))
            nc++;
        else if ((x1 >= 0.0) || (x2 >= 0.0))
            if ((x1 - y1 * (x2 - x1) / (y2 - y1)) > 0.0)
                nc++;
        sh = nsh;
    }
    
    if (y3 >= 0.0)
        nsh = 1;
    else 
        nsh = -1;
    
    if (sh != nsh) {
        if ((x2 >= 0.0) && (x3 >= 0.0))
            nc++;
        else if ((x2 >= 0.0) || (x3 >= 0.0))
            if ((x2 - y2 * (x3 - x2) / (y3 - y2)) > 0.0)
                nc++;
        sh = nsh;
    }
    
    if (y1 >= 0.0)
        nsh = 1;
    else 
        nsh = -1;
    
    if (sh != nsh) {
        if ((x3 >= 0.0) && (x1 >= 0.0))
            nc++;
        else if ((x3 >= 0.0) || (x1 >= 0.0))
            if ((x1 - y1 * (x1 - x3) / (y1 - y3)) > 0.0)
                nc++;
        sh = nsh;
    }
    
    if ((nc % 2) == 0) 
        return(0);
    else {
        return(1);
    }
    
}




#define DEST_FOUND (1)
#define DEST_NOT_FOUND (0)

#define ON_THE_PLANE          (0)
#define POSITIVE_SUBTREE_ONLY (1)
#define NEGATIVE_SUBTREE_ONLY (2)
#define POSITIVE_SIDE_FIRST   (3)
#define NEGATIVE_SIDE_FIRST   (4)



/****************************************************************************
 *
 *    traverse_bsp()
 *    traverse_subtree()
 *
 *    Traverse the BSP tree. The traversal is in-order. Since the traversal
 *    of the BSP tree begins at the middle of the BSP tree (i.e., the source
 *    node), the traversal is performed as follows.
 *      1) Traverse the positive subtee of the start (source) node.
 *      2) For each ancestor node of the source node, visit it (immediate
 *         parent first).
 *         2.1) Test if the node intercepts the ray.
 *         2.2) Traverse the subtree of the node (i.e., the subtree that the
 *              source node does not belong to.
 *
 *    traverse_bsp() takes care of the traversal of ancestor nodes. Ordinary
 *    traversal of a subtree is done by traverse_subtree().
 *
 *****************************************************************************/


int traverse_bsp( src_node, p, ray, r_min, r_max, process_id )
  
  Patch *src_node ;
  Vertex *p ;
  Ray *ray ;
  float r_min, r_max ;
  unsigned process_id;
{
    float t ;
    Patch *parent, *visited_child ;
    int traverse_subtree() ;
    int test_intersection() ;
    int advice ;
    
    
    /* (1) Check patch cache */
    if( check_patch_cache( p, ray, r_min, r_max, process_id ) )
        return( 1 ) ;
    
    /* (2) Check S+(src_node) */
    if( traverse_subtree( src_node->bsp_positive, p, ray, r_min, r_max, process_id ) )
        return( 1 ) ;
    
    /* (3) Continue in-order traversal till root is encountered */
    for( parent = src_node->bsp_parent, visited_child = src_node ;
        parent ;
        visited_child = parent, parent = parent->bsp_parent )
        {
            /* Check intersection at this node */
            advice = intersection_type( parent, p, ray, &t, r_min, r_max, process_id ) ;
            if( (advice != POSITIVE_SUBTREE_ONLY) &&
               (advice != NEGATIVE_SUBTREE_ONLY) )
                {
                    if( test_intersection( parent, p, ray, t, process_id ) )
                        return( 1 ) ;
                    
                    r_min = t - VIS_RANGE_MARGIN ;
                }
            
            /* Traverse unvisited subtree of the node */
            if(   (parent->bsp_positive == visited_child)
               && (advice != POSITIVE_SUBTREE_ONLY) )
                {
                    if( traverse_subtree( parent->bsp_negative, p, ray,
                                         r_min, r_max, process_id ) )
                        return( 1 ) ;
                }
            else if( (parent->bsp_positive != visited_child)
                    && (advice != NEGATIVE_SUBTREE_ONLY) )
                {
                    if( traverse_subtree( parent->bsp_positive, p, ray,
                                         r_min, r_max, process_id ) )
                        return( 1 ) ;
                }
        }
    
    return( 0 ) ;
}




int traverse_subtree( node, p, ray, r_min, r_max, process_id )
  /*
   *    To minimize the length of the traversal of the BSP tree, a pruning
   *    algorithm is incorporated.
   *    One possibility (not used in this version) is to prune one of the 
   *	 subtrees if the node in question intersects the ray outside of the 
   *	 range defined by the source and the destination patches.
   *    Another possibility (used here) is more aggressive pruning. Like the above
   *    method, the intersection point is checked against the range to prune the
   *    subtree. But instead of using a constant source-destination range,
   *    the range itself is recursively subdivided so that the minimum range is
   *    applied the possibility of pruning maximized.
   */
  Patch *node ;
  Vertex *p ;
  Ray *ray ;
  float r_min, r_max ;
  unsigned process_id;
{
    float t ;
    int advice ;
    
    
    if( node == 0 )
        return( 0 ) ;
    
    advice = intersection_type( node, p, ray, &t, r_min, r_max, process_id ) ;
    if( advice == POSITIVE_SIDE_FIRST )
        {
            /* The ray is approaching from the positive side of the patch */
            if( traverse_subtree( node->bsp_positive, p, ray,
                                 r_min, t + VIS_RANGE_MARGIN, process_id ) )
                return( 1 ) ;
            
            if( test_intersection( node, p, ray, t, process_id ) )
                return( 1 ) ;
            return( traverse_subtree( node->bsp_negative, p, ray,
                                     t - VIS_RANGE_MARGIN, r_max, process_id ) ) ;
        }
    else if( advice == NEGATIVE_SIDE_FIRST )
        {
            /* The ray is approaching from the negative side of the patch */
            if( traverse_subtree( node->bsp_negative, p, ray,
                                 r_min, t + VIS_RANGE_MARGIN, process_id ) )
                return( 1 ) ;
            if( test_intersection( node, p, ray, t, process_id ) )
                return( 1 ) ;
            
            return( traverse_subtree( node->bsp_positive, p, ray,
                                     t - VIS_RANGE_MARGIN, r_max, process_id ) ) ;
        }
    
    else if( advice == POSITIVE_SUBTREE_ONLY )
        return( traverse_subtree( node->bsp_positive, p, ray,
                                 r_min, r_max, process_id ) ) ;
    else if( advice == NEGATIVE_SUBTREE_ONLY )
        return( traverse_subtree( node->bsp_negative, p, ray,
                                 r_min, r_max, process_id ) ) ;
    else
        /* On the plane */
        return( 1 ) ;
}



/**************************************************************************
 *
 *	intersection_type()
 *
 *       Compute intersection coordinate as the barycentric coordinate
 *       w.r.t the ray vector. This value is returned to the caller through
 *       the variable passed by reference.
 *       intersection_type() also classifies the intersection type and
 *       returns the type as the "traversal advice" code.
 *       Possible types are:
 *       1) the patch and the ray are parallel
 *          --> POSITIVE_SUBTREE_ONLY, NEGATIVE_SUBTREE_ONLY, or ON_THE_PLANE
 *       2) intersects the ray outside of the specified range
 *          --> POSITIVE_SUBTREE_ONLY, NEGATIVE_SUBTREE_ONLY
 *       3) intersects within the range
 *          --> POSITIVE_SIDE_FIRST, NEGATIVE_SIDE_FIRST
 *
 ***************************************************************************/

int intersection_type( patch, p, ray, tval, range_min, range_max, process_id )
  
  Patch  *patch ;
  Vertex *p ;
  Ray    *ray ;
  float  *tval ;
  float  range_min, range_max ;
  unsigned process_id;
{
    float r_dot_n ;
    float dist ;
    float t ;
    float nx, ny, nz ;
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    vis_struct[process_id].bsp_nodes_visited++ ;
#endif
    
    /* (R.N) */
    nx = patch->plane_equ.n.x ;
    ny = patch->plane_equ.n.y ;
    nz = patch->plane_equ.n.z ;
    
    r_dot_n = nx * ray->x + ny * ray->y + nz * ray->z ;
    dist = patch->plane_equ.c  +  p->x * nx  +  p->y * ny  +  p->z * nz ;
    
    if( (-(float)F_ZERO < r_dot_n) && (r_dot_n < (float)F_ZERO) )
        {
            if( dist > (float)F_COPLANAR )
                return( POSITIVE_SUBTREE_ONLY ) ;
            else if( dist < -F_COPLANAR )
                return( NEGATIVE_SUBTREE_ONLY ) ;
            
            return( ON_THE_PLANE ) ;
        }
    
    t = -dist / r_dot_n ;
    *tval = t ;
    
    if( t < range_min )
        {
            if( r_dot_n >= 0 )
                return( POSITIVE_SUBTREE_ONLY ) ;
            else
                return( NEGATIVE_SUBTREE_ONLY ) ;
        }
    else if ( t > range_max )
        {
            if( r_dot_n >= 0 )
                return( NEGATIVE_SUBTREE_ONLY ) ;
            else
                return( POSITIVE_SUBTREE_ONLY ) ;
        }
    else
        {
            if( r_dot_n >= 0 )
                return( NEGATIVE_SIDE_FIRST ) ;
            else
                return( POSITIVE_SIDE_FIRST ) ;
        }
}


/*************************************************************
 *
 *	test_intersection()
 *
 *************************************************************/

int test_intersection( patch, p, ray, tval, process_id )
  
  Patch  *patch ;
  Vertex *p ;
  Ray    *ray ;
  float  tval ;
  unsigned process_id;
{
    void update_patch_cache() ;
    
    /* Rays always hit the destination. Note that (R.Ndest) is already
       checked by visibility() */
    
    if( patch == vis_struct[process_id].v_dest_patch )
        {
            vis_struct[process_id].pool_dst_hits++ ;
            return( 1 ) ;
        }
    
    if( patch_tested( patch, process_id ) )
        return( 0 ) ;
    
    if( v_intersect( patch, p, ray, tval, process_id ) )
        {
            /* Store it in the patch-cache */
            update_patch_cache( patch, process_id ) ;
            return( 1 ) ;
        }
    
    return( 0 ) ;
}



/*************************************************************
 *
 *	patch_cache
 *
 *       update_patch_cache()
 *       check_patch_cache()
 *       init_patch_cache()
 *
 *    To exploit visibility coherency, a patch cache is used.
 *    Before traversing the BSP tree, the cache contents are tested to see
 *    if they intercept the ray in question. The size of the patch cache is
 *    defined by PATCH_CACHE_SIZE (in patch.H). Since the first two 
 *    entries of the cache 
 *    usually cover about 95% of the cache hits, increasing the cache size
 *    does not help much. Nevertheless, the program is written so that
 *    increasing cache size does not result in additional ray-intersection
 *    test.
 *
 *************************************************************/

void update_patch_cache( patch, process_id )
  
  Patch *patch ;
  unsigned process_id;
{
    int i ;
    
    /* Shift current contents */
    for( i = PATCH_CACHE_SIZE-1 ; i > 0  ; i-- )
        vis_struct[process_id].patch_cache[i] = vis_struct[process_id].patch_cache[i-1] ;
    
    /* Store the new patch data */
    vis_struct[process_id].patch_cache[0] = patch ;
}



int check_patch_cache( p, ray, r_min, r_max, process_id )
  
  Vertex *p ;
  Ray    *ray ;
  float  r_min, r_max ;
  unsigned process_id;
{
    int i ;
    float t ;
    Patch *temp ;
    int advice ;
    
    for( i = 0 ; i < PATCH_CACHE_SIZE ; i++ )
        {
            if(   (vis_struct[process_id].patch_cache[i] == 0)
               || (vis_struct[process_id].patch_cache[i] == vis_struct[process_id].v_dest_patch)
               || (vis_struct[process_id].patch_cache[i] == vis_struct[process_id].v_src_patch) )
                continue ;
            
            advice = intersection_type( vis_struct[process_id].patch_cache[i],  p, ray, &t,
                                       r_min, r_max, process_id ) ;
            
            /* If no intersection, then skip */
            if(   (advice == POSITIVE_SUBTREE_ONLY)
               || (advice == NEGATIVE_SUBTREE_ONLY) )
                continue ;
            
            if(   (advice == ON_THE_PLANE)
               || v_intersect( vis_struct[process_id].patch_cache[i], p, ray, t, process_id ) )
                {
                    /* Change priority */
                    if( i > 0 )
                        {
                            temp = vis_struct[process_id].patch_cache[ i-1 ] ;
                            vis_struct[process_id].patch_cache[ i-1 ] = vis_struct[process_id].patch_cache[ i ] ;
                            vis_struct[process_id].patch_cache[ i ] = temp ;
                        }
                    
                    return( 1 ) ;
                }
        }
    
    
    return( 0 ) ;
}



void init_patch_cache(process_id)
{
    int i ;
    
    for( i = 0 ; i < PATCH_CACHE_SIZE ; i++ )
        vis_struct[process_id].patch_cache[ i ] = 0 ;
}


int patch_tested( p, process_id )
  
  Patch *p ;
  unsigned process_id;
{
    int i ;
    
    for( i = 0 ; i < PATCH_CACHE_SIZE ; i++ )
        {
            if( p == vis_struct[process_id].patch_cache[i] )
                return( 1 ) ;
        }
    
    return( 0 ) ;
}


/*************************************************************
 *
 *	float visibility(): checking if two patches are mutually invisible.
 *
 *************************************************************/


float visibility( e1, e2, n_rays, process_id )
  
  Element *e1, *e2;
  int n_rays ;
  unsigned process_id;
{
    float range_max, range_min ;
    int i;
    Ray *r;
    int ray_reject ;
    
    vis_struct[process_id].v_src_patch  = e1->patch;
    vis_struct[process_id].v_dest_patch = e2->patch;
    
    vis_struct[process_id].v_src_p1 = e1->ev1->p ;
    vis_struct[process_id].v_src_v12.x = e1->ev2->p.x - vis_struct[process_id].v_src_p1.x ;
    vis_struct[process_id].v_src_v12.y = e1->ev2->p.y - vis_struct[process_id].v_src_p1.y ;
    vis_struct[process_id].v_src_v12.z = e1->ev2->p.z - vis_struct[process_id].v_src_p1.z ;
    vis_struct[process_id].v_src_v13.x = e1->ev3->p.x - vis_struct[process_id].v_src_p1.x ;
    vis_struct[process_id].v_src_v13.y = e1->ev3->p.y - vis_struct[process_id].v_src_p1.y ;
    vis_struct[process_id].v_src_v13.z = e1->ev3->p.z - vis_struct[process_id].v_src_p1.z ;
    
    vis_struct[process_id].v_dest_p1 = e2->ev1->p ;
    vis_struct[process_id].v_dest_v12.x = e2->ev2->p.x - vis_struct[process_id].v_dest_p1.x ;
    vis_struct[process_id].v_dest_v12.y = e2->ev2->p.y - vis_struct[process_id].v_dest_p1.y ;
    vis_struct[process_id].v_dest_v12.z = e2->ev2->p.z - vis_struct[process_id].v_dest_p1.z ;
    vis_struct[process_id].v_dest_v13.x = e2->ev3->p.x - vis_struct[process_id].v_dest_p1.x ;
    vis_struct[process_id].v_dest_v13.y = e2->ev3->p.y - vis_struct[process_id].v_dest_p1.y ;
    vis_struct[process_id].v_dest_v13.z = e2->ev3->p.z - vis_struct[process_id].v_dest_p1.z ;
    
    get_test_rays( vis_struct[process_id].point_pool, vis_struct[process_id].ray_pool, n_rays, process_id ) ;
    
    range_min = -VIS_RANGE_MARGIN ;
    range_max =  1.0 + VIS_RANGE_MARGIN ;
    
    vis_struct[process_id].pool_dst_hits = 0 ;
    ray_reject = 0 ;
    for ( i = 0 ; i < n_rays ; i++ )
        {
            r = &(vis_struct[process_id].ray_pool[i]);
            
            if (  (inner_product( r, &(vis_struct[process_id].v_src_patch)->plane_equ.n ) <= 0.0 )
                ||(inner_product( r, &(vis_struct[process_id].v_dest_patch)->plane_equ.n ) >= 0.0 ) )
                {
                    ray_reject++ ;
                    continue ;
                }
            
            traverse_bsp( vis_struct[process_id].v_src_patch, &vis_struct[process_id].point_pool[i], r, range_min, range_max, process_id ) ;
        }
    
    if (ray_reject == n_rays) {
        /* All rays have been trivially rejected. This can occur
           if no rays are shot between visible portion of the elements.
           Return partial visibility (1/No-of-rays). */
        
        /* Return partially visible result */
        vis_struct[process_id].pool_dst_hits = 1 ;
    }
    
    return( (float)vis_struct[process_id].pool_dst_hits / (float)n_rays ) ;
}



/*****************************************************************
 *
 *    compute_visibility_values()
 *
 *    Apply visibility() function to an interaction list.
 *
 ******************************************************************/

void compute_visibility_values( elem, inter, n_inter, process_id )
  
  Element *elem ;                /* Element that the list belongs to */
  Interaction *inter ;	   /* Interaction list */
  int n_inter ;		   /* Number of interactions whose visibility
                          value is computed */
  unsigned process_id;
{
    for( ; n_inter > 0 ; inter = inter->next, n_inter-- )
        {
            if( inter->visibility != VISIBILITY_UNDEF )
                continue ;
            
            vis_struct[process_id].bsp_nodes_visited = 0 ;
            
            inter->visibility
                = visibility( elem, inter->destination,
                             N_VISIBILITY_TEST_RAYS, process_id ) ;
            
            vis_struct[process_id].total_bsp_nodes_visited += vis_struct[process_id].bsp_nodes_visited ;
        }
}


/*****************************************************************
 *
 *    visibility_task()
 *
 *    Compute visibility values and then call continuation when all
 *    the undefined visibility values have been computed.
 *
 ******************************************************************/

void visibility_task( elem, inter, n_inter, k, process_id )
  
  Element *elem ;                /* Element that the list belongs to */
  Interaction *inter ;	   /* Interaction list */
  int n_inter ;		   /* Number of interactions whose visibility
                          value is computed */
  void (*k)() ;		   /* Continuation */
  unsigned process_id;
{
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    Patch_Cost *pc ;
#endif
    int new_vis_undef_count ;
    
    /* Compute visibility */
    vis_struct[process_id].total_bsp_nodes_visited = 0 ;
    compute_visibility_values( elem, inter, n_inter, process_id ) ;
    
    /* Change visibility undef count */
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    elem->n_vis_undef_inter -= n_inter ;
    new_vis_undef_count = elem->n_vis_undef_inter ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    pc = &global->patch_cost[ elem->patch->seq_no ] ;
    {pthread_mutex_lock(&(pc->cost_lock->lock));};
    pc->n_bsp_node += vis_struct[process_id].total_bsp_nodes_visited ;
    {pthread_mutex_unlock(&(pc->cost_lock->lock));};
#endif
    
    /* Call continuation if this is the last task finished. */
    if( new_vis_undef_count == 0 )
        k( elem, process_id ) ;
}
