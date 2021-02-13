
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

/*
 *	Element management.
 *
 *       This module has the following functionalities.
 *       (1) Creation/initialization of a new instance of an element object.
 *       (2) Recursive refinement of elements.
 */


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



/***************************************************************************
 ****************************************************************************
 *
 *    Methods for Element object
 *
 ****************************************************************************
 ****************************************************************************
 ****************************************************************************
 *
 *    foreach_element_in_patch()
 *    foreach_leaf_element_in_patch()
 *
 *    General purpose driver. For each element of a patch, apply specified
 *    function.  The function is passed a pointer to the element.
 *    Traversal is DFS (post-order).
 *
 ****************************************************************************/

static void _foreach_element(), _foreach_leaf_element() ;
  
  
  void foreach_element_in_patch( patch, func, arg1, process_id )
  
  Patch *patch ;
  void (*func)() ;
  void*  arg1;
  unsigned process_id;
{
    void _foreach_element() ;
    
    _foreach_element( patch->el_root, func, arg1, process_id ) ;
}


static void _foreach_element( elem, func, arg1, process_id )
  
  Element *elem ;
  void (*func)() ;
  void*  arg1;
  unsigned process_id;
{
    if( elem == 0 )
        return ;
    
    /* Process children */
    if( ! LEAF_ELEMENT( elem ) )
        {
            _foreach_element( elem->center, func, arg1, process_id ) ;
            _foreach_element( elem->top,    func, arg1, process_id ) ;
            _foreach_element( elem->left,   func, arg1, process_id ) ;
            _foreach_element( elem->right,  func, arg1, process_id ) ;
        }
    
    /* Apply function to this node */
    func( elem, arg1, process_id ) ;
}


void foreach_leaf_element_in_patch( patch, func, arg1, process_id )
  
  Patch *patch ;
  void (*func)() ;
  void*  arg1;
  unsigned process_id;
{
    void _foreach_leaf_element() ;
    
    _foreach_leaf_element( patch->el_root, func, arg1, process_id ) ;
}


static void _foreach_leaf_element( elem, func, arg1, process_id )
  
  Element *elem ;
  void (*func)() ;
  void*  arg1;
{
    if( elem == 0 )
        return ;
    
    /* Process children */
    if( LEAF_ELEMENT( elem ) )
        func( elem, arg1, process_id ) ;
    else
        {
            _foreach_leaf_element( elem->center, func, arg1, process_id ) ;
            _foreach_leaf_element( elem->top,    func, arg1, process_id ) ;
            _foreach_leaf_element( elem->left,   func, arg1, process_id ) ;
            _foreach_leaf_element( elem->right,  func, arg1, process_id ) ;
        }
}



/***************************************************************************
 *
 *    ff_refine_elements()
 *
 *    Recursively refine the elements based on FF estimate.
 *
 ****************************************************************************/

void    ff_refine_elements( e1, e2, level, process_id )
  
  Element *e1, *e2 ;		     /* A pair of elements to be refined */
  int level ;			     /* Interaction level */
  unsigned process_id;
{
    int subdiv_advice ;
    Interaction i12, i21 ;
    Interaction *inter ;
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
#if defined(SUN4)
    Patch_Cost *pc1, *pc2 ;
#else
    volatile Patch_Cost *pc1, *pc2 ;
#endif
    int cost1, cost2 ;
#endif
    
    
    /* Now compute formfactor.
       As the BSP tree is being modified at this moment, don't test
       visibility. */
    compute_formfactor( e1, e2, &i12, process_id ) ;
    compute_formfactor( e2, e1, &i21, process_id ) ;
    
    /* Analyze the error of FF */
    subdiv_advice = error_analysis( e1, e2, &i12, &i21, process_id ) ;
    
    /* Execute subdivision procedure */
    if( NO_INTERACTION(subdiv_advice) )
        /* Two elements are mutually invisible. Do nothing */
        return ;
    
    else if( NO_REFINEMENT_NECESSARY(subdiv_advice) )
        {
            /* Create links and finish the job */
            inter = get_interaction(process_id) ;
            *inter = i12 ;
            inter->visibility = VISIBILITY_UNDEF ;
            insert_vis_undef_interaction( e1, inter, process_id ) ;
            
            inter = get_interaction(process_id) ;
            *inter = i21 ;
            inter->visibility = VISIBILITY_UNDEF ;
            insert_vis_undef_interaction( e2, inter, process_id ) ;
            
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
            /* Update cost variable */
            pc1 = &global->patch_cost[ e1->patch->seq_no ] ;
            pc2 = &global->patch_cost[ e2->patch->seq_no ] ;
            if( pc1->n_total_inter <= 13 )
                cost1 = (int)ceil(e1->area / Area_epsilon) ;
            else
                cost1 = 1 ;
            
            if( pc2->n_total_inter <= 13 )
                cost2 = (int)ceil(e2->area / Area_epsilon) ;
            else
                cost2 = 1 ;
            
            {pthread_mutex_lock(&(global->cost_sum_lock));};
            pc1->cost_estimate += cost1 ;
            pc1->n_total_inter++ ;
            pc2->cost_estimate += cost2 ;
            pc2->n_total_inter++ ;
            global->cost_estimate_sum += (cost1 + cost2) ;
            global->cost_sum += (cost1 + cost2) ;
            {pthread_mutex_unlock(&(global->cost_sum_lock));};
#endif
        }
    
    else if( REFINE_PATCH_1(subdiv_advice) )
        {
            /* Refine patch 1 */
            subdivide_element( e1, process_id ) ;
            
            /* Locally solve it */
            ff_refine_elements( e1->top,    e2, level+1, process_id ) ;
            ff_refine_elements( e1->center, e2, level+1, process_id ) ;
            ff_refine_elements( e1->left,   e2, level+1, process_id ) ;
            ff_refine_elements( e1->right,  e2, level+1, process_id ) ;
        }
    else
        {
            /* Refine patch 2 */
            subdivide_element( e2, process_id ) ;
            
            /* Locally solve it */
            ff_refine_elements( e1, e2->top,    level+1, process_id ) ;
            ff_refine_elements( e1, e2->center, level+1, process_id ) ;
            ff_refine_elements( e1, e2->left,   level+1, process_id ) ;
            ff_refine_elements( e1, e2->right,  level+1, process_id ) ;
        }
}



/***************************************************************************
 *
 *    error_analysis()
 *
 *    Analyze FF error.
 *
 ****************************************************************************/

int error_analysis( e1, e2, inter12, inter21, process_id )
  Element *e1, *e2 ;		       /* Element pair */
  Interaction *inter12, *inter21 ;   /* Interaction */
  unsigned process_id;
{
    int cc ;
    
    /* Check visibility */
    cc = patch_intersection( &e1->patch->plane_equ,
                            &e2->ev1->p, &e2->ev2->p, &e2->ev3->p, process_id ) ;
    if( NEGATIVE_SIDE(cc) )
        /* If negative or on the plane, then do nothing */
        return( _NO_INTERACTION ) ;
    
    cc = patch_intersection( &e2->patch->plane_equ,
                            &e1->ev1->p, &e1->ev2->p, &e1->ev3->p, process_id ) ;
    if( NEGATIVE_SIDE(cc) )
        /* If negative or on the plane, then do nothing */
        return( _NO_INTERACTION ) ;
    
    return( _NO_REFINEMENT_NECESSARY ) ;
}





/***************************************************************************
 *
 *    bf_refine_element()
 *
 *    Recursively refine the elements based on BF estimate.
 *    Return value: number of interactions newly created.
 *
 ****************************************************************************/

static int  bf_refine_element( subdiv, elem, inter, process_id )
  
  int subdiv ;		      /* Subdivision code */
  Element *elem ;		      /* Element to be refined */
  Interaction *inter ;	      /* Interaction to be refined */
  unsigned process_id;
{
    Element *e_dst = inter->destination ;
    Interaction *pi ;
    float visibility_val ;
    int new_inter = 0 ;
    
    
    visibility_val = NO_VISIBILITY_NECESSARY(subdiv)?
        (float)1.0 : VISIBILITY_UNDEF ;
    
    if( REFINE_PATCH_1(subdiv) )
        {
            /* Refine this element */
            /* (1) Make sure it has children */
            subdivide_element( elem, process_id ) ;
            
            /* (2) For each of the patch, create an interaction */
            if( element_completely_invisible( elem->center, e_dst, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem->center, e_dst, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem->center, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem->top, e_dst, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem->top, e_dst, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem->top, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem->left, e_dst, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem->left, e_dst, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem->left, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem->right, e_dst, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem->right, e_dst, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem->right, pi, process_id ) ;
                    new_inter++ ;
                }
        }
    else
        {
            /* Refine source element */
            /* (1) Make sure it has children */
            subdivide_element( e_dst, process_id ) ;
            
            /* (2) Insert four new interactions
               NOTE: Use *inter as a place holder to link 4 new interactions
               since *prev may be NULL */
            
            if( element_completely_invisible( elem, e_dst->center, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem, e_dst->center, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem, e_dst->top, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem, e_dst->top, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem, e_dst->left, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem, e_dst->left, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem, pi, process_id ) ;
                    new_inter++ ;
                }
            if( element_completely_invisible( elem, e_dst->right, process_id ) == 0 )
                {
                    pi = get_interaction(process_id) ;
                    compute_formfactor( elem, e_dst->right, pi, process_id ) ;
                    pi->visibility = visibility_val ;
                    insert_vis_undef_interaction( elem, pi, process_id ) ;
                    new_inter++ ;
                }
        }
    
    return( new_inter ) ;
}




/***************************************************************************
 *
 *    bf_error_analysis_list()
 *
 *    For each interaction in the list, analyze error of BF value.
 *    If error is lower than the limit, the interaction is linked to the
 *    normal interaction list. Otherwise, BF-refinement is performed and
 *    the newly created interactions are linked to the vis-undef-list.
 *
 ****************************************************************************/

void bf_error_analysis_list( elem, i_list, process_id )
  
  Element *elem ;		     /* Element to be analyzed */
  Interaction *i_list ;	     /* Top of the interaction list */
  unsigned process_id;
{
    int subdiv_advice ;
    Interaction *prev = 0 ;
    Interaction *inter = i_list ;
    Interaction *refine_inter ;
    int i_len = 0 ;
    int delta_n_inter = 0 ;
    
    
    while( inter )
        {
            /* Analyze error */
            subdiv_advice = bf_error_analysis( elem, inter, process_id ) ;
            
            if( NO_REFINEMENT_NECESSARY(subdiv_advice) )
                {
                    /* Go on to the next interaction */
                    prev = inter ;
                    inter = inter->next ;
                    i_len++ ;
                }
            else
                {
                    /* Remove this interaction from the list */
                    refine_inter = inter ;
                    inter = inter->next ;
                    if( prev == 0 )
                        i_list = inter ;
                    else
                        prev->next = inter ;
                    
                    /* Perform refine */
                    delta_n_inter += bf_refine_element( subdiv_advice,
                                                       elem, refine_inter, process_id ) ;
                    
                    /* Delete this inter anyway */
                    free_interaction( refine_inter, process_id ) ;
                    delta_n_inter-- ;
                }
        }
    
    /* Link good interactions to elem->intearctions */
    if( i_len > 0 )
        {
            {pthread_mutex_lock(&(elem->elem_lock->lock));};
            prev->next = elem->interactions ;
            elem->interactions = i_list ;
            elem->n_interactions += i_len ;
            {pthread_mutex_unlock(&(elem->elem_lock->lock));};
        }
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    /* Update patch interaction count */
    if( delta_n_inter != 0 )
        {
            Patch_Cost *pc ;
            
            pc = &global->patch_cost[ elem->patch->seq_no ] ;
            {pthread_mutex_lock(&(pc->cost_lock->lock));};
            pc->n_total_inter += delta_n_inter ;
            {pthread_mutex_unlock(&(pc->cost_lock->lock));};
        }
#endif
}



/***************************************************************************
 *
 *    bf_error_analysis()
 *
 *    Analyze error of BF value.
 *    Return value: subdivision advice code.
 *
 ****************************************************************************/

int bf_error_analysis( elem, inter, process_id )
  Element *elem ;		     /* Elem that the interaction belongs to */
  Interaction *inter ;	     /* Interaction to be analyzed */
  unsigned process_id;
{
    float rad_avg ;		     /* Average radiosity */
    float total_error ;
    float visibility_error ;
    int   vis_code = 0 ;
    
    /* Compute amount of error associated with the BF.
       
       FFcomputed = (F + Ferr)(V + Verr) = FV + (Ferr V + F Verr + Ferr Verr)
       BFcomputed = BF + BFerr
       = B FV + B (Ferr V + F Verr + Ferr Verr)
       */
    
    if( (0.0 < inter->visibility) && (inter->visibility < 1.0) )
        visibility_error = 1.0 ;
    else
        visibility_error = FF_VISIBILITY_ERROR ;
    
    rad_avg =( inter->destination->rad.r
              + inter->destination->rad.g
              + inter->destination->rad.b ) * (float)(1.0 / 3.0) ;
    
    total_error = (inter->visibility * inter->formfactor_err
                   + visibility_error * inter->formfactor_out
                   + visibility_error * inter->formfactor_err) * rad_avg ;
    
    /* If BF is smaller than the threshold, then not subdivide */
    if( (total_error <= BFepsilon) && (elem->n_interactions <= 10) )
        return( _NO_REFINEMENT_NECESSARY ) ;
    else if( total_error <= BFepsilon * 0.5 )
        return( _NO_REFINEMENT_NECESSARY ) ;
    
    /* Subdivide light source or receiver whichever is larger */
    if( elem->area > inter->destination->area )
        {
            if( elem->area > Area_epsilon )
                /* Subdivide this element (receiver) */
                return( _REFINE_PATCH_1 | vis_code ) ;
        }
    else
        {
            if( inter->destination->area > Area_epsilon )
                
                /* Subdivide source element */
                return( _REFINE_PATCH_2 | vis_code ) ;
        }
    
    return( _NO_REFINEMENT_NECESSARY ) ;
}





/***************************************************************************
 *
 *    radiosity_converged()
 *
 *    Return TRUE(1) when the average radiosity value is converged.
 *
 ****************************************************************************/

int radiosity_converged(process_id)
  unsigned process_id;
{
    float prev_total, current_total ;
    float difference ;
    Rgb rad ;
    
    /* Check radiosity value */
    prev_total = global->prev_total_energy.r + global->prev_total_energy.g
        + global->prev_total_energy.b ;
    current_total = global->total_energy.r + global->total_energy.g
        + global->total_energy.b ;
    
    /* Compute difference from the previous iteration */
    prev_total += 1.0e-4 ;
    difference = fabs( (current_total / prev_total) - (float)1.0 ) ;
    
    if( verbose_mode )
        {
            rad = global->total_energy ;
            rad.r /= global->total_patch_area ;
            rad.g /= global->total_patch_area ;
            rad.b /= global->total_patch_area ;
            printf( "Total energy:     " ) ;
            print_rgb( &global->total_energy, process_id ) ;
            printf( "Average radiosity:" ) ;
            print_rgb( &rad, process_id ) ;
            printf( "Difference %.2f%%\n", difference * 100.0 ) ;
        }
    
    if( difference <=  Energy_epsilon )
        return( 1 ) ;
    else
        return( 0 ) ;
}


/***************************************************************************
 *
 *    subdivide_element()
 *
 *    Create child elements. If they already exist, do nothing.
 *
 ****************************************************************************/

void subdivide_element( e, process_id )
  
  Element *e ;		     /* Parent element of the new children */
  unsigned process_id;
{
    float quarter_area ;
    ElemVertex *ev_12, *ev_23, *ev_31 ;
    Edge *e_12_23, *e_23_31, *e_31_12 ;
    Element *enew, *ecenter ;
    int rev_12, rev_23, rev_31 ;
    
    /* Lock the element before checking the value */
    {pthread_mutex_lock(&(e->elem_lock->lock));};
    
    /* Check if the element already has children */
    if( ! _LEAF_ELEMENT(e) )
        {
            {pthread_mutex_unlock(&(e->elem_lock->lock));};
            return ;
        }
    
    /* Subdivide edge structures */
    subdivide_edge( e->e12, (float)0.5, process_id ) ;
    subdivide_edge( e->e23, (float)0.5, process_id ) ;
    subdivide_edge( e->e31, (float)0.5, process_id ) ;
    ev_12 = e->e12->ea->pb ;
    ev_23 = e->e23->ea->pb ;
    ev_31 = e->e31->ea->pb ;
    
    /* Then create new edges */
    e_12_23 = create_edge( ev_12, ev_23, process_id ) ;
    e_23_31 = create_edge( ev_23, ev_31, process_id ) ;
    e_31_12 = create_edge( ev_31, ev_12, process_id ) ;
    
    /* Area parameters */
    quarter_area = e->area * (float)0.25 ;
    
    /* (1) Create the center patch */
    enew = get_element(process_id) ;
    ecenter = enew ;
    enew->parent= e ;
    enew->patch = e->patch ;
    enew->ev1   = ev_23 ;
    enew->ev2   = ev_31 ;
    enew->ev3   = ev_12 ;
    enew->e12   = e_23_31 ;
    enew->e23   = e_31_12 ;
    enew->e31   = e_12_23 ;
    enew->area  = quarter_area ;
    enew->rad   = e->rad ;
    
    /* (2) Create the top patch */
    rev_12 = EDGE_REVERSE( e->e12, e->ev1, e->ev2 ) ;
    rev_23 = EDGE_REVERSE( e->e23, e->ev2, e->ev3 ) ;
    rev_31 = EDGE_REVERSE( e->e31, e->ev3, e->ev1 ) ;
    
    enew = get_element(process_id) ;
    e->top = enew ;
    enew->parent= e ;
    enew->patch = e->patch ;
    enew->ev1   = e->ev1 ;
    enew->ev2   = ev_12 ;
    enew->ev3   = ev_31 ;
    enew->e12   = (!rev_12)? e->e12->ea : e->e12->eb ;
    enew->e23   = e_31_12 ;
    enew->e31   = (!rev_31)? e->e31->eb : e->e31->ea ;
    enew->area  = quarter_area ;
    enew->rad   = e->rad ;
    
    /* (3) Create the left patch */
    enew = get_element(process_id) ;
    e->left = enew ;
    enew->parent= e ;
    enew->patch = e->patch ;
    enew->ev1   = ev_12 ;
    enew->ev2   = e->ev2 ;
    enew->ev3   = ev_23 ;
    enew->e12   = (!rev_12)? e->e12->eb : e->e12->ea ;
    enew->e23   = (!rev_23)? e->e23->ea : e->e23->eb ;
    enew->e31   = e_12_23 ;
    enew->area  = quarter_area ;
    enew->rad   = e->rad ;
    
    /* (4) Create the right patch */
    enew = get_element(process_id) ;
    e->right = enew ;
    enew->parent= e ;
    enew->patch = e->patch ;
    enew->ev1   = ev_31 ;
    enew->ev2   = ev_23 ;
    enew->ev3   = e->ev3 ;
    enew->e12   = e_23_31 ;
    enew->e23   = (!rev_23)? e->e23->eb : e->e23->ea ;
    enew->e31   = (!rev_31)? e->e31->ea : e->e31->eb ;
    enew->area  = quarter_area ;
    enew->rad   = e->rad ;
    
    /* Finally, set e->center */
    e->center = ecenter ;
    
    /* Unlock the element */
    {pthread_mutex_unlock(&(e->elem_lock->lock));};
}


/***************************************************************************
 *
 *    process_rays()
 *
 *    Gather rays.
 *
 *    Solution strategy:
 *            Original radiosity equation:    [I-M] B = E
 *            Iterative solution:                           2    3
 *                                            B = [I + M + M  + M  ...] E
 *            Sollution by this routine:      Bn = E + M Bn-1
 *                                            (B0 = E)
 *
 *
 *    process_rays() first checks the amount of error associated with each
 *    interaction. If it is large, then the interaction is refined.
 *    After all the interactions are examined, the light energy is
 *    gathered. This gathered energy and the energy passed from the ancestors
 *    are added and pushed down to descendants. The descendants in turn pop up
 *    the area weighted energy gathered at descendant level. Thus, the 
 *    radiosity at level i is:
 *          Bi =  E
 *              + Sum(j=0 to i-1) gather(j)        --- gathered at ancestors
 *              + gather(i)                        --- gathered at this level
 *              + 1/4( Bi+1.right + Bi+1.left + Bi+1.top + Bi+1.center)
 *                                                 --- gathered at descendants
 *
 *    The implementation is slightly complicated due to many fork and join
 *    operations that occur when:
 *      (1) four children are visited and the results are added to Bi.
 *      (2) parallel visibility computation is done and then process_rays() resumed.
 *
 *    The former case would be a simple (non-tail)-recursive code in a
 *    uniprocessor program. However, because we create tasks to process lower
 *    levels and the creation of tasks does not mean the completion of
 *    processing at the lower level, we use a mechanism to suspend
 *    processing at this level and later resume it after the child tasks are
 *    finished.
 *    To be portable across many systems, in this implementation procedures
 *    are split at the point of task creation so that the subtask can call
 *    the next portion of the procedure on exit of the task.
 *    This mechanism can be best explained as the continuation-passing-style
 *    (CPS) where continuation means a lambda expression which represents 'the
 *    rest of the program'.
 *
 ****************************************************************************/


static  void process_rays2(), process_rays3() ;
  static  void gather_rays() ;
  static  void elem_join_operation() ;
  
  
  
  void process_rays( e, process_id )
  
  Element *e ;
  unsigned process_id;
{
    Interaction *i_list ;
    
    /* Detach interactions from the list */
    {pthread_mutex_lock(&(e->elem_lock->lock));};
    i_list = e->interactions ;
    e->interactions = (Interaction *)0 ;
    e->n_interactions = 0 ;
    {pthread_mutex_unlock(&(e->elem_lock->lock));};
    
    /* For each interaction, do BF-error-analysis */
    bf_error_analysis_list( e, i_list, process_id ) ;
    
    if( e->n_vis_undef_inter == 0 )
        process_rays3( e, process_id ) ;
    else
        /* Some interactions were refined.
           Compute visibility and do BF-analysis again */
        create_visibility_tasks( e, process_rays2, process_id ) ;
}




static void process_rays2( e, process_id )
  
  Element *e ;
  unsigned process_id;
{
    Interaction *i_list ;
    
    /* Detach interactions from the vis-undef-list. They now have their
       visibility computed */
    {pthread_mutex_lock(&(e->elem_lock->lock));};
    i_list = e->vis_undef_inter ;
    e->vis_undef_inter = (Interaction *)0 ;
    e->n_vis_undef_inter = 0 ;
    {pthread_mutex_unlock(&(e->elem_lock->lock));};
    
    /* For each interaction, do BF-error-analysis */
    bf_error_analysis_list( e, i_list, process_id ) ;
    
    if( e->n_vis_undef_inter == 0 )
        process_rays3( e, process_id ) ;
    else
        /* Some interactions were refined.
           Compute visibility and do BF-analysis again */
        create_visibility_tasks( e, process_rays2, process_id ) ;
}



static void process_rays3( e, process_id )
  
  Element *e ;
  unsigned process_id;
{
    Rgb rad_push ;		          /* Radiosity value pushed down */
    
    
    /* Gather light rays that impinge on this element */
    gather_rays( e, process_id ) ;
    
    /* Now visit children */
    if( ! LEAF_ELEMENT(e) )
        {
            /* Compute radiosity that is pushed to descendents */
            rad_push.r = e->rad_in.r + e->rad_subtree.r ;
            rad_push.g = e->rad_in.g + e->rad_subtree.g ;
            rad_push.b = e->rad_in.b + e->rad_subtree.b ;
            
            e->center->rad_in = rad_push ;
            e->top->   rad_in = rad_push ;
            e->right-> rad_in = rad_push ;
            e->left->  rad_in = rad_push ;
            e->join_counter = 4 ;
            
            /* Create tasks to process children */
            create_ray_task( e->center, process_id ) ;
            create_ray_task( e->top, process_id ) ;
            create_ray_task( e->left, process_id ) ;
            create_ray_task( e->right, process_id ) ;
            
            /* The rest of the job (pop up the computed radiosity) is
               handled by the continuation function, elem_join_operation().
               It is called when the lower level finishes.
               On exit, elem_join_operation() calls the continuation of the
               parent, which is also elem_join_operation().
               To be general, those tasks should be passed the continuation
               (elem_join_operation). But, since the continuation is obvious
               in this context, it is hardwired below */
        }
    else
        {
            /* Update element radiosity at the leaf level */
            e->rad.r = e->rad_in.r + e->rad_subtree.r + e->patch->emittance.r ;
            e->rad.g = e->rad_in.g + e->rad_subtree.g + e->patch->emittance.g ;
            e->rad.b = e->rad_in.b + e->rad_subtree.b + e->patch->emittance.b ;
            
            /* Ship out radiosity to the parent */
            elem_join_operation( e->parent, e, process_id ) ;
        }
}



/***************************************************************************
 *
 *    elem_join_operation()
 *
 *    This function performs the second half of the function of process_rays.
 *    That is, i)   add area weighted child radiosity
 *             ii)  update radiosity variable
 *             iii) call continuation of the parent (ie, elem_join_operation
 *                  itself)
 *
 *    Thus, in spirit, the function is a tail recursive routine although
 *    tail-recursion-elimination is manually performed in the code below.
 *
 ****************************************************************************/



static void elem_join_operation( e, ec, process_id )
  
  Element *e ;			  /* This element */
  Element *ec ;			  /* Child element */
  unsigned process_id;
{
    int join_flag ;
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    Patch_Cost *pc ;
#endif
    
    
    while( e != 0 )
        {
            /* Get radiosity of the child and add to my radiosity */
            {pthread_mutex_lock(&(e->elem_lock->lock));};
            e->rad_subtree.r += ec->rad_subtree.r * (float)0.25 ;
            e->rad_subtree.g += ec->rad_subtree.g * (float)0.25 ;
            e->rad_subtree.b += ec->rad_subtree.b * (float)0.25 ;
            e->join_counter-- ;
            join_flag = (e->join_counter == 0) ;
            {pthread_mutex_unlock(&(e->elem_lock->lock));};
            
            if( join_flag == 0 )
                /* Other children are not finished. Return. */
                return ;
            
            /* This is the continuation called by the last (4th) subprocess.
               Perform JOIN at this level */
            /* Update element radiosity */
            e->rad.r = e->rad_in.r + e->rad_subtree.r + e->patch->emittance.r ;
            e->rad.g = e->rad_in.g + e->rad_subtree.g + e->patch->emittance.g ;
            e->rad.b = e->rad_in.b + e->rad_subtree.b + e->patch->emittance.b ;
            
            /* Traverse the tree one level up and repeat */
            ec = e ;
            e = e->parent ;
        }
    
    /* Process RAY root level finished. Update energy variable */
    {pthread_mutex_lock(&(global->avg_radiosity_lock));};
    global->total_energy.r += ec->rad.r * ec->area ;
    global->total_energy.g += ec->rad.g * ec->area ;
    global->total_energy.b += ec->rad.b * ec->area ;
    global->total_patch_area += ec->area ;
    {pthread_mutex_unlock(&(global->avg_radiosity_lock));};
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    /* Then update the cost variable of the patch */
    pc = &global->patch_cost[ ec->patch->seq_no ] ;
    pc->cost_history[3] = pc->cost_history[2] ;
    pc->cost_history[2] = pc->cost_history[1] ;
    pc->cost_history[1] = pc->cost_history[0] ;
    pc->cost_history[0] = PATCH_COST( pc ) ;
    pc->cost_estimate   = PATCH_COST_ESTIMATE( pc ) ;
    
    /* Also, update the global cost variable */
    {pthread_mutex_lock(&(global->cost_sum_lock));};
    global->cost_sum          += pc->cost_history[0] ;
    global->cost_estimate_sum += pc->cost_estimate ;
    {pthread_mutex_unlock(&(global->cost_sum_lock));};
#endif
    
}



/***************************************************************************
 *
 *    gather_rays()
 *
 ****************************************************************************/

static void gather_rays( elem, process_id )
  
  Element *elem ;
  unsigned process_id;
{
    Rgb rad_elem ;		/* Radiosity gathered by this elem */
    float ff_v ;		/* Form factor times visibility */
    float bf_r, bf_g, bf_b ;
    Interaction *inter ;
    
    
    /* Return immediately if there is no interaction */
    if( (inter = elem->interactions) == 0 )
        {
            elem->rad_subtree.r = 0.0 ;
            elem->rad_subtree.g = 0.0 ;
            elem->rad_subtree.b = 0.0 ;
            return ;
        }
    
    
    /* Gather rays of this element
       (do it directly without the driver function, 'Foreach-interaction') */
    rad_elem.r = 0.0 ;
    rad_elem.g = 0.0 ;
    rad_elem.b = 0.0 ;
    
    while( inter )
        {
            /* Be careful !
               Use FF(out) to compute incoming energy */
            ff_v = inter->formfactor_out * inter->visibility ;
            bf_r = ff_v * inter->destination->rad.r ;
            bf_g = ff_v * inter->destination->rad.g ;
            bf_b = ff_v * inter->destination->rad.b ;
            
            rad_elem.r += bf_r ;
            rad_elem.g += bf_g ;
            rad_elem.b += bf_b ;
            
            /* Update pointers */
            inter = inter->next ;
        }
    
    /* Multiply the gathered radiosity by the diffuse color of this element */
    rad_elem.r *= elem->patch->color.r ;
    rad_elem.g *= elem->patch->color.g ;
    rad_elem.b *= elem->patch->color.b ;
    
    /* Store the value at the initial value of 'rad_subtree' */
    elem->rad_subtree = rad_elem ;
}




/***************************************************************************
 *
 *    elememnt_completely_invisible()
 *
 *    Fast primary visibility test.
 *
 ****************************************************************************/

int element_completely_invisible( e1, e2, process_id )
  
  Element *e1, *e2 ;		    /* Two elements to be tested */
  unsigned process_id;
{
    int cc ;
    
    /* Check visibility */
    cc = patch_intersection( &e1->patch->plane_equ,
                            &e2->ev1->p, &e2->ev2->p, &e2->ev3->p, process_id ) ;
    if( NEGATIVE_SIDE(cc) )
        /* If negative or on the plane, then do nothing */
        return( 1 ) ;
    
    cc = patch_intersection( &e2->patch->plane_equ,
                            &e1->ev1->p, &e1->ev2->p, &e1->ev3->p, process_id ) ;
    if( NEGATIVE_SIDE(cc) )
        /* If negative or on the plane, then do nothing */
        return( 1 ) ;
    
    return( 0 ) ;
}


/***************************************************************************
 *
 *    get_element()
 *
 *    Create a new instance of Element
 *
 ****************************************************************************/

Element *get_element(process_id)
  unsigned process_id;
{
    Element *p ;
    
    /* Lock the free list */
    {pthread_mutex_lock(&(global->free_element_lock));};
    
    /* Test pointer */
    if( global->free_element == 0 )
        {
            printf( "Fatal: Ran out of element buffer\n" ) ;
            {pthread_mutex_unlock(&(global->free_element_lock));};
            exit( 1 ) ;
        }
    
    /* Get an element data structure */
    p = global->free_element ;
    global->free_element = p->center ;
    global->n_free_elements-- ;
    
    /* Unlock the list */
    {pthread_mutex_unlock(&(global->free_element_lock));};
    
    /* Clear pointers just in case.. */
    p->parent             = 0 ;
    p->center             = 0 ;
    p->top                = 0 ;
    p->right              = 0 ;
    p->left               = 0 ;
    p->interactions       = 0 ;
    p->n_interactions     = 0 ;
    p->vis_undef_inter    = 0 ;
    p->n_vis_undef_inter  = 0 ;
    
    return( p ) ;
}



/***************************************************************************
 *
 *    leaf_element()
 *
 *    Returns TRUE(1) if this element is a leaf.
 *
 *    For strong and processor memory consistency models, this routine is not
 *    necessary. For weak and release consistency models, elem->center must be
 *    tested within the Lock.
 *
 ****************************************************************************/

int leaf_element( elem, process_id )
  
  Element *elem ;
  unsigned process_id;
{
    int leaf ;
    
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    leaf  = _LEAF_ELEMENT(elem) ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
    
    return( leaf ) ;
}


/***************************************************************************
 *
 *    init_elemlist()
 *
 *    Initialize Element free list
 *
 ****************************************************************************/

void init_elemlist(process_id)
  unsigned process_id;
{
    int i ;
    
    /* Initialize Element free list */
    for( i = 0 ; i < MAX_ELEMENTS-1 ; i++ )
        {
            global->element_buf[i].center = &global->element_buf[i+1] ;
            /* Initialize lock variable */
            global->element_buf[i].elem_lock
                = get_sharedlock( SHARED_LOCK_SEG1, process_id ) ;
        }
    global->element_buf[ MAX_ELEMENTS-1 ].center = 0 ;
    global->element_buf[ MAX_ELEMENTS-1 ].elem_lock
        = get_sharedlock( SHARED_LOCK_SEG1, process_id ) ;
    
    global->free_element = global->element_buf ;
    global->n_free_elements = MAX_ELEMENTS ;
    {pthread_mutex_init(&(global->free_element_lock), NULL);};
}

/***************************************************************************
 *
 *    print_element()
 *
 *    Print contents of an element.
 *
 ****************************************************************************/

void print_element( elem, process_id )
  
  Element *elem ;
  unsigned process_id;
{
    printf( "Element (%x)\n", (int)elem ) ;
    
    print_point( &elem->ev1->p, process_id ) ;
    print_point( &elem->ev2->p, process_id ) ;
    print_point( &elem->ev3->p, process_id ) ;
    
    printf( "Radiosity:" ) ;     print_rgb( &elem->rad, process_id ) ;
}






/***************************************************************************
 ****************************************************************************
 *
 *    Methods for Interaction object
 *
 ****************************************************************************
 ****************************************************************************/
/***************************************************************************
 *
 *    foreach_interaction_in_element()
 *
 *    General purpose driver. For each interaction of the element, apply
 *    specified function.  The function is passed a pointer to the interaction.
 *    Traversal is linear.
 *
 ****************************************************************************/

void foreach_interaction_in_element( elem, func, arg1, process_id )
  
  Element *elem ;
  void (*func)() ;
  void*  arg1;
  unsigned process_id;
{
    Interaction *inter ;
    
    if( elem == 0 )
        return ;
    
    for( inter = elem->interactions ; inter ; inter = inter->next )
        func( elem, inter, arg1, process_id ) ;
}



/***************************************************************************
 *
 *    compute_interaction()
 *    compute_formfactor()
 *
 *    compute_interaction() computes all the interaction parameters and fills
 *    in the entries of Interaction data structure.
 *    compute_formfactor() computes the formfactor only.
 *
 ****************************************************************************/

static float compute_diff_disc_formfactor() ;
  static float compute_diff_diff_formfactor() ;
  
  
  void compute_formfactor( e_src, e_dst, inter, process_id )
  
  Element *e_src ;		    /* Source of the interaction */
  Element *e_dst ;		    /* Destination of the interaction */
  Interaction *inter ;	    /* Interaction data structure */
  unsigned process_id;
{
    float ff_c, ff_1, ff_2, ff_3 ;
    float ff_c1, ff_c2, ff_c3, ff_avg ;
    Vertex pc_src, pc_dst ;
    Vertex pc1_src, pc2_src, pc3_src ;
    float ff_min, ff_max, ff_err ;
    
    /* Estimate FF using disk approximation */
    /* (1) Compute FF(diff-disc) from the center of src to the destination */
    four_center_points( &e_src->ev1->p, &e_src->ev2->p, &e_src->ev3->p,
                       &pc_src, &pc1_src, &pc2_src, &pc3_src ) ;
    center_point( &e_dst->ev1->p, &e_dst->ev2->p, &e_dst->ev3->p, &pc_dst ) ;
    ff_c  = compute_diff_disc_formfactor( &pc_src,  e_src, &pc_dst, e_dst, process_id ) ;
    ff_c1 = compute_diff_disc_formfactor( &pc1_src, e_src, &pc_dst, e_dst, process_id ) ;
    ff_c2 = compute_diff_disc_formfactor( &pc2_src, e_src, &pc_dst, e_dst, process_id ) ;
    ff_c3 = compute_diff_disc_formfactor( &pc3_src, e_src, &pc_dst, e_dst, process_id ) ;
    if( ff_c  < 0 ) ff_c  = 0 ;
    if( ff_c1 < 0 ) ff_c1 = 0 ;
    if( ff_c2 < 0 ) ff_c2 = 0 ;
    if( ff_c3 < 0 ) ff_c3 = 0 ;
    ff_avg = (ff_c + ff_c1 + ff_c2 + ff_c3) * (float)0.25 ;
    ff_min = ff_max = ff_c ;
    if( ff_min > ff_c1 ) ff_min = ff_c1 ;
    if( ff_min > ff_c2 ) ff_min = ff_c2 ;
    if( ff_min > ff_c3 ) ff_min = ff_c3 ;
    if( ff_max < ff_c1 ) ff_max = ff_c1 ;
    if( ff_max < ff_c2 ) ff_max = ff_c2 ;
    if( ff_max < ff_c3 ) ff_max = ff_c3 ;
    
    /* (2) Compute FF(diff-disc) from the 3 vertices of the source */
    ff_1 = compute_diff_disc_formfactor( &e_src->ev1->p, e_src,
                                        &pc_dst, e_dst, process_id ) ;
    ff_2 = compute_diff_disc_formfactor( &e_src->ev2->p, e_src,
                                        &pc_dst, e_dst, process_id ) ;
    ff_3 = compute_diff_disc_formfactor( &e_src->ev3->p, e_src,
                                        &pc_dst, e_dst, process_id ) ;
    
    /* (3) Find FF min and max */
    ff_min = ff_max = ff_c ;
    if( ff_min > ff_1 ) ff_min = ff_1 ;
    if( ff_min > ff_2 ) ff_min = ff_2 ;
    if( ff_min > ff_3 ) ff_min = ff_3 ;
    if( ff_max < ff_1 ) ff_max = ff_1 ;
    if( ff_max < ff_2 ) ff_max = ff_2 ;
    if( ff_max < ff_3 ) ff_max = ff_3 ;
    
    /* (4) Clip FF(diff-disc) if it is negative */
    if( ff_avg < 0 )
        ff_avg = 0 ;
    inter->formfactor_out = ff_avg ;
    
    /* (5) Then find maximum difference from the FF at the center */
    ff_err = (ff_max - ff_avg) ;
    if( ff_err < (ff_avg - ff_min) )
        ff_err = ff_avg - ff_min ;
    inter->formfactor_err = ff_err ;
    
    /* (6) Correct visibility if partially visible */
    if( (ff_avg < 0) && (inter->visibility == 0) )
        /* All ray missed the visible portion of the elements.
           Set visibility to a non-zero value manually */
        /** inter->visibility = FF_VISIBILITY_ERROR **/ ;
    
    /* (7) Fill destination */
    inter->destination = e_dst ;
}


static float _diff_disc_formfactor( p, e_src, p_disc, area, normal, process_id )
  
  Vertex *p ;
  Element *e_src ;
  Vertex *p_disc ;
  float area ;
  Vertex *normal ;
  unsigned process_id;
{
    Vertex vec_sd ;
    float dist_sq ;
    float fnorm ;
    float  cos_s, cos_d, angle_factor ;
    
    vec_sd.x = p_disc->x - p->x ;
    vec_sd.y = p_disc->y - p->y ;
    vec_sd.z = p_disc->z - p->z ;
    dist_sq = vec_sd.x*vec_sd.x + vec_sd.y*vec_sd.y + vec_sd.z*vec_sd.z ;
    
    fnorm = area / ((float)M_PI * dist_sq  + area) ;
    
    /* (2) Now, consider angle to the other patch from the normal. */
    normalize_vector( &vec_sd, &vec_sd ) ;
    cos_s =  inner_product( &vec_sd, &e_src->patch->plane_equ.n ) ;
    cos_d = -inner_product( &vec_sd, normal ) ;
    angle_factor = cos_s * cos_d ;
    
    /* Return the form factor */
    return( fnorm * angle_factor ) ;
}


static float compute_diff_disc_formfactor( p, e_src, p_disc, e_dst, process_id )
  
  Vertex *p ;
  Element *e_src ;
  Vertex *p_disc ;
  Element *e_dst ;
  unsigned process_id;
{
    Vertex p_c, p_c1, p_c2, p_c3 ;
    float quarter_area ;
    float ff_c, ff_c1, ff_c2, ff_c3 ;
    float ff_dbg ;
    
    four_center_points( &e_dst->ev1->p, &e_dst->ev2->p, &e_dst->ev3->p,
                       &p_c, &p_c1, &p_c2, &p_c3 ) ;
    
    quarter_area = e_dst->area * (float)0.25 ;
    
    ff_c = _diff_disc_formfactor( p, e_src, &p_c,  quarter_area,
                                 &e_dst->patch->plane_equ.n, process_id ) ;
    ff_c1= _diff_disc_formfactor( p, e_src, &p_c1, quarter_area,
                                 &e_dst->patch->plane_equ.n, process_id ) ;
    ff_c2= _diff_disc_formfactor( p, e_src, &p_c2, quarter_area,
                                 &e_dst->patch->plane_equ.n, process_id ) ;
    ff_c3= _diff_disc_formfactor( p, e_src, &p_c3, quarter_area,
                                 &e_dst->patch->plane_equ.n, process_id ) ;
    
    if( ff_c  < 0 ) ff_c  = 0 ;
    if( ff_c1 < 0 ) ff_c1 = 0 ;
    if( ff_c2 < 0 ) ff_c2 = 0 ;
    if( ff_c3 < 0 ) ff_c3 = 0 ;
    
    return( ff_c + ff_c1 + ff_c2 + ff_c3 ) ;
}


static float compute_diff_diff_formfactor( p_src, e_src, p_dst, e_dst, process_id )
  
  Vertex *p_src ;
  Element *e_src ;
  Vertex *p_dst ;
  Element *e_dst ;
  unsigned process_id;
{
    Vertex vec_sd ;
    float dist_sq ;
    float fnorm ;
    float  cos_s, cos_d, angle_factor ;
    
    vec_sd.x = p_dst->x - p_src->x ;
    vec_sd.y = p_dst->y - p_src->y ;
    vec_sd.z = p_dst->z - p_src->z ;
    dist_sq = vec_sd.x*vec_sd.x + vec_sd.y*vec_sd.y + vec_sd.z*vec_sd.z ;
    if( dist_sq < F_ZERO )
        return( 0.0 ) ;
    fnorm = (float)1.0 / ((float)M_PI * dist_sq) ;
    
    /* (2) Now, consider angle to the other patch from the normal. */
    normalize_vector( &vec_sd, &vec_sd ) ;
    cos_s =  inner_product( &vec_sd, &e_src->patch->plane_equ.n ) ;
    cos_d = -inner_product( &vec_sd, &e_dst->patch->plane_equ.n ) ;
    angle_factor = cos_s * cos_d ;
    
    /* Return the form factor */
    return( fnorm * angle_factor ) ;
}


void compute_interaction( e_src, e_dst, inter, subdiv, process_id )
  
  Element *e_src ;		    /* Source of the interaction */
  Element *e_dst ;		    /* Destination of the interaction */
  Interaction *inter ;	    /* Interaction data structure */
  int subdiv ;		    /* Subdivision code */
  unsigned process_id;
{
    /* (1) Check visibility. */
    if( NO_VISIBILITY_NECESSARY(subdiv) )
        inter->visibility = 1.0 ;
    else
        inter->visibility = VISIBILITY_UNDEF ;
    
    
    /* (2) Compute formfactor */
    compute_formfactor( e_src, e_dst, inter, process_id ) ;
}



/***************************************************************************
 *
 *    insert_interaction()
 *    delete_interaction()
 *    insert_vis_undef_interaction()
 *    delete_vis_undef_interaction()
 *
 *    Insert/Delete interaction from the interaction list.
 *
 ****************************************************************************/


void insert_interaction( elem, inter, process_id )
  
  Element *elem ;		     /* Interacting elements */
  Interaction *inter ;
  unsigned process_id;
{
    /* Link from patch 1 to patch 2 */
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    inter->next = elem->interactions ;
    elem->interactions = inter ;
    elem->n_interactions++ ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
}



void delete_interaction( elem, prev, inter, process_id )
  
  Element *elem ;
  Interaction *prev, *inter ;
  unsigned process_id;
{
    /* Remove from the list */
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    if( prev == 0 )
        elem->interactions = inter->next ;
    else
        prev->next = inter->next ;
    elem->n_interactions-- ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
    
    /* Return to the free list */
    free_interaction( inter, process_id ) ;
}



void insert_vis_undef_interaction( elem, inter, process_id )
  
  Element *elem ;		     /* Interacting elements */
  Interaction *inter ;
  unsigned process_id;
{
    /* Link from patch 1 to patch 2 */
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    inter->next = elem->vis_undef_inter ;
    elem->vis_undef_inter = inter ;
    elem->n_vis_undef_inter++ ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
}

void delete_vis_undef_interaction( elem, prev, inter, process_id )
  
  Element *elem ;
  Interaction *prev, *inter ;
  unsigned process_id;
{
    /* Remove from the list */
    {pthread_mutex_lock(&(elem->elem_lock->lock));};
    if( prev == 0 )
        elem->vis_undef_inter = inter->next ;
    else
        prev->next = inter->next ;
    elem->n_vis_undef_inter-- ;
    {pthread_mutex_unlock(&(elem->elem_lock->lock));};
}


/***************************************************************************
 *
 *    get_interaction()
 *    free_interaction()
 *
 *    Create/delete an instance of an interaction.
 *
 ****************************************************************************/

Interaction *get_interaction(process_id)
  unsigned process_id;
{
    Interaction *p ;
    
    /* Lock the free list */
    {pthread_mutex_lock(&(global->free_interaction_lock));};
    
    /* Test pointer */
    if( global->free_interaction == 0 )
        {
            printf( "Fatal: Ran out of interaction buffer\n" ) ;
            {pthread_mutex_unlock(&(global->free_interaction_lock));};
            exit( 1 ) ;
        }
    
    /* Get an element data structure */
    p = global->free_interaction ;
    global->free_interaction = p->next ;
    global->n_free_interactions-- ;
    
    /* Unlock the list */
    {pthread_mutex_unlock(&(global->free_interaction_lock));};
    
    /* Clear pointers just in case.. */
    p->next   = 0 ;
    p->destination = 0 ;
    
    
    return( p ) ;
}



void free_interaction( interaction, process_id )
  
  Interaction *interaction ;
  unsigned process_id;
{
    /* Lock the free list */
    {pthread_mutex_lock(&(global->free_interaction_lock));};
    
    /* Get a task data structure */
    interaction->next = global->free_interaction ;
    global->free_interaction = interaction ;
    global->n_free_interactions++ ;
    
    /* Unlock the list */
    {pthread_mutex_unlock(&(global->free_interaction_lock));};
}


/***************************************************************************
 *
 *    init_interactionlist()
 *
 *    Initialize Interaction free list
 *
 ****************************************************************************/

void init_interactionlist(process_id)
  unsigned process_id;
{
    int i ;
    
    /* Initialize Interaction free list */
    for( i = 0 ; i < MAX_INTERACTIONS-1 ; i++ )
        global->interaction_buf[i].next = &global->interaction_buf[i+1] ;
    global->interaction_buf[ MAX_INTERACTIONS-1 ].next = 0 ;
    global->free_interaction = global->interaction_buf ;
    global->n_free_interactions = MAX_INTERACTIONS ;
    {pthread_mutex_init(&(global->free_interaction_lock), NULL);};
}


/***************************************************************************
 *
 *    print_interaction()
 *
 *    Print interaction data structure.
 *
 ****************************************************************************/

void print_interaction( inter, process_id )
  
  Interaction *inter ;
  unsigned process_id;
{
    
    printf( "Interaction(0x%x)\n", (int)inter ) ;
    printf( "    Dest: Elem (0x%x) of patch %d\n",
           (int)inter->destination, inter->destination->patch->seq_no ) ;
    printf( "    Fout: %f    Vis: %f\n",
           inter->formfactor_out,
           inter->visibility ) ;
    printf( "    Next: 0x%x\n", inter->next ) ;
}
