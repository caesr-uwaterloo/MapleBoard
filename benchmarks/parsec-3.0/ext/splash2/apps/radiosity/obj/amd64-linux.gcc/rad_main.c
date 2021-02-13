
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
 *       Parallel Hierarchical Radiosity
 *
 *	    Main program
 *
 ***************************************************************/

#include <stdio.h>
#if defined(SGI_GL)
#include <gl.h>
#if defined(GL_NASA)
#include <panel.h>
#endif
#endif

/* ANL macro initialization */


#include <pthread.h>
#include <sys/time.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#define MAX_THREADS 1024
pthread_t PThreadTable[MAX_THREADS];
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

  
  
  
  /***************************************
   *
   *    Global shared variables
   *
   ****************************************/
  
  Global *global;
  Timing **timing;
  
  
  /***************************************
   *
   *    Global variables (not shared)
   *
   ****************************************/
  
  int   n_processors              = DEFAULT_N_PROCESSORS ;
  int   n_taskqueues              = DEFAULT_N_TASKQUEUES ;
  int   n_tasks_per_queue         = DEFAULT_N_TASKS_PER_QUEUE ;
  int   N_inter_parallel_bf_refine= DEFAULT_N_INTER_PARALLEL_BF_REFINEMENT ;
  int   N_visibility_per_task     = DEFAULT_N_VISIBILITY_PER_TASK ;
  float Area_epsilon              = DEFAULT_AREA_EPSILON ;
  float Energy_epsilon            = DEFAULT_ENERGY_EPSILON ;
  float BFepsilon                 = DEFAULT_BFEPSILON ;
  
  int batch_mode = 0 ;
  int verbose_mode = 0 ;
  
  /*
    in converting from a fork process model to an sproc (threads) model,
    taskqueue_id and time_process_start are converted to individual arrays
    without worrying about false sharing.  This is because taskqueue_id is 
    read-only  once written by the parent process, and time_process_start 
    is written only once by each process.
    */
  
  int taskqueue_id[MAX_PROCESSORS] ; 		/* Task queue ID */
  long time_rad_start, time_rad_end, time_process_start[MAX_PROCESSORS] ;
  
  
  /*********************************************************
   *
   *    Global variables (used only by the master process)
   *
   **********************************************************/
  
#define N_SLIDERS (5)
  
  void change_view_x(), change_view_y(), change_view_zoom() ;
  void change_BFepsilon(), change_EXepsilon(), change_area_epsilon() ;
  
  slider sliders[N_SLIDERS] = {
      { "View(X)  deg ", -100,  100, (int)DFLT_VIEW_ROT_X,  5,  change_view_x },
      { "View(Y)  deg ", -100,  100, (int)DFLT_VIEW_ROT_Y,  5,  change_view_y },
      { "View(Zoom)   ",   0,  50, (int)DFLT_VIEW_ZOOM*10,6,  change_view_zoom },
      { "BF-e      0.1%",  0,  50,  (int)(DEFAULT_BFEPSILON *1000.0),
            11, change_BFepsilon },
      { "Area-e       ",   0, 5000, (int)DEFAULT_AREA_EPSILON,
            11, change_area_epsilon },
  } ;
  
#define N_CHOICES (4)
  
#define CHOICE_RAD_RUN    (0)
#define CHOICE_RAD_STEP   (1)
#define CHOICE_RAD_RESET  (2)
  
#define CHOICE_DISP_RADIOSITY   (0)
#define CHOICE_DISP_SHADED      (1)
#define CHOICE_DISP_PATCH       (2)
#define CHOICE_DISP_MESH        (3)
#define CHOICE_DISP_INTERACTION (4)
  
#define CHOICE_UTIL_PS        (0)
#define CHOICE_UTIL_STAT_CRT  (1)
#define CHOICE_UTIL_STAT_FILE (2)
#define CHOICE_UTIL_CLEAR_RAD (3)
  
  void change_display() ;
  void start_radiosity() ;
  void select_model() ;
  void utility_tools() ;
  
  choice choices[N_CHOICES] = {
      { "Run",
            { "Run", "Step", "Reset", 0 },
            0, start_radiosity }, 
      { "Display",
            { "Filled",   "Smooth shading", "Show polygon edges",
                  "Show element edges",  "Show interactions", 0 },
            0, change_display }, 
      { "Models",
            { "Test", "Room", "LargeRoom", 0 },
            0, select_model }, 
      { "Tools",
            { "HardCopy(PS)", "Statistics", "Statistics(file)",
                  "Clear Radiosity Value", 0 },
            0, utility_tools }, 
  } ;
  
  static void expose_callback() ;
  
  /***************************************
   *
   *    Main function.
   *
   ****************************************/
  
  static void parse_args() ;
  
  static int dostats = 0;
  
  main( ac, av )
  
  int ac ;
  char *av[] ;
{
    void radiosity(), parse_args() ;
    int i;
    unsigned long int total_rad_time, max_rad_time, min_rad_time;
    unsigned long int total_refine_time, max_refine_time, min_refine_time;
    unsigned long int total_wait_time, max_wait_time, min_wait_time;
    unsigned long int total_vertex_time, max_vertex_time, min_vertex_time;
    
    /* Parse arguments */
    parse_args( ac, av ) ;
    choices[2].init_value = model_selector ;
    
    /* Initialize graphic device */
    if( batch_mode == 0 )
        {
            g_init( ac, av ) ;
            setup_view( DFLT_VIEW_ROT_X, DFLT_VIEW_ROT_Y,
                       DFLT_VIEW_DIST, DFLT_VIEW_ZOOM,0 ) ;
        }
    
    /* Initialize ANL macro */
    {;} ;
    
    /* Allocate global shared memory and initialize */
    global = (Global *) malloc(sizeof(Global)); ;
    if( global == 0 )
        {
            printf( "Can't allocate memory\n" ) ;
            exit(1) ;
        }
    init_global(0) ;
    
    timing = (Timing **) malloc(n_processors * sizeof(Timing *));;
    for (i = 0; i < n_processors; i++)
        timing[i] = (Timing *) malloc(sizeof(Timing));;
    
    /* Initialize shared lock */
    init_sharedlock(0) ;
    
    /* Initial random testing rays array for visibility test. */
    init_visibility_module(0) ;
    
/* POSSIBLE ENHANCEMENT:  Here is where one might distribute the 
   sobj_struct, task_struct, and vis_struct data structures across 
   physically distributed memories as desired.

   One way to place data is as follows:

   int i;

   for (i=0;i<n_processors;i++) {
     Place all addresses x such that
       &(sobj_struct[i]) <= x < &(sobj_struct[i+1]) on node i
     Place all addresses x such that
       &(task_struct[i]) <= x < &(task_struct[i+1]) on node i
     Place all addresses x such that
       &(vis_struct[i]) <= x < &(vis_struct[i+1]) on node i
   }

*/

    if( batch_mode )
        {
            /* In batch mode, create child processes and start immediately */
            
            /* Time stamp */
            {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(time_rad_start ) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
            
            global->index = 0;
            for( i = 0 ; i < n_processors ; i++ )
                {
                    taskqueue_id[i] = assign_taskq(0) ;

                }
            
            printf("HI %d\n", n_processors);
            fflush(stdout);
            {
	long	i, Error;

	for (i = 0; i < (n_processors) - 1; i++) {
		Error = pthread_create(&PThreadTable[i], NULL, (void * (*)(void *))(radiosity), NULL);
		if (Error != 0) {
			printf("Error in pthread_create().\n");
			exit(-1);
		}
	}

	radiosity();
};
            /* And start processing */
            //taskqueue_id[n_processors] = assign_taskq(0) ;
            //radiosity() ;
            
            printf("HI Done\n");
            fflush(stdout);
            {
	unsigned long	i, Error;
	for (i = 0; i < (n_processors) - 1; i++) {
		Error = pthread_join(PThreadTable[i], NULL);
		if (Error != 0) {
			printf("Error in pthread_join().\n");
			exit(-1);
		}
	}
};
            
            /* Time stamp */
            {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(time_rad_end ) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
            
            /* Print out running time */
            printf("TIMING STATISTICS MEASURED BY MAIN PROCESS:\n");
            
            print_running_time(0);
            
            if (dostats) {
                printf("\n\n\nPER-PROCESS STATISTICS:\n");
                
                printf("%8s%20s%20s%12s%12s\n","Proc","Total","Refine","Wait","Smooth");
                printf("%8s%20s%20s%12s%12s\n\n","","Time","Time","Time","Time");
                for (i = 0; i < n_processors; i++)
                    printf("%8d%20lu%20lu%12lu%12lu\n",i,timing[i]->rad_time, timing[i]->refine_time, timing[i]->wait_time, timing[i]->vertex_time);
                
                total_rad_time = timing[0]->rad_time;
                max_rad_time = timing[0]->rad_time;
                min_rad_time = timing[0]->rad_time;
                
                total_refine_time = timing[0]->refine_time;
                max_refine_time = timing[0]->refine_time;
                min_refine_time = timing[0]->refine_time;
                
                total_wait_time = timing[0]->wait_time;
                max_wait_time = timing[0]->wait_time;
                min_wait_time = timing[0]->wait_time;
                
                total_vertex_time = timing[0]->vertex_time;
                max_vertex_time = timing[0]->vertex_time;
                min_vertex_time = timing[0]->vertex_time;
                
                for (i = 1; i < n_processors; i++) {
                    total_rad_time += timing[i]->rad_time;
                    if (timing[i]->rad_time > max_rad_time)
                        max_rad_time = timing[i]->rad_time;
                    if (timing[i]->rad_time < min_rad_time)
                        min_rad_time = timing[i]->rad_time;
                    
                    total_refine_time += timing[i]->refine_time;
                    if (timing[i]->refine_time > max_refine_time)
                        max_refine_time = timing[i]->refine_time;
                    if (timing[i]->refine_time < min_refine_time)
                        min_refine_time = timing[i]->refine_time;
                    
                    total_wait_time += timing[i]->wait_time;
                    if (timing[i]->wait_time > max_wait_time)
                        max_wait_time = timing[i]->wait_time;
                    if (timing[i]->wait_time < min_wait_time)
                        min_wait_time = timing[i]->wait_time;
                    
                    total_vertex_time += timing[i]->vertex_time;
                    if (timing[i]->vertex_time > max_vertex_time)
                        max_vertex_time = timing[i]->vertex_time;
                    if (timing[i]->vertex_time < min_vertex_time)
                        min_vertex_time = timing[i]->vertex_time;
                }
                
                printf("\n\n%8s%20lu%20lu%12lu%12lu\n","Max", max_rad_time, max_refine_time, max_wait_time, max_vertex_time);
                printf("\n%8s%20lu%20lu%12lu%12lu\n","Min", min_rad_time, min_refine_time, min_wait_time, min_vertex_time);
                printf("\n%8s%20lu%20lu%12lu%12lu\n","Avg", (int) (((double) total_rad_time) / ((double) (1.0 * n_processors))), (int) (((double) total_refine_time) / ((double) (1.0 * n_processors))), (int) (((double) total_wait_time) / ((double) (1.0 * n_processors))), (int) (((double) total_vertex_time) / ((double) (1.0 * n_processors))));
                printf("\n\n");
                
            }
            
            /*	print_fork_time(0) ; */
            
            print_statistics( stdout, 0 ) ;
        }
    else
        {
            /* In interactive mode, start workers, and the master starts
               notification loop */
            
            /* Start notification loop */
            g_start( expose_callback,
                    N_SLIDERS, sliders, N_CHOICES, choices ) ;
        }
    {exit(0);};
    exit(0) ;
}



/***************************************
 *
 *    PANEL call back routine
 *
 *    start_radiosity()   (MASTER only)
 *
 ****************************************/

static int disp_fill_switch = 1 ;
static int disp_shade_switch = 0 ;
static int disp_fill_mode = 1 ;
static int disp_patch_switch = 0 ;
static int disp_mesh_switch  = 0 ;
static int disp_interaction_switch = 0 ;
static int disp_crnt_view_x = (int)DFLT_VIEW_ROT_X ;
  static int disp_crnt_view_y = (int)DFLT_VIEW_ROT_Y ;
  static float disp_crnt_zoom = DFLT_VIEW_ZOOM ;
  
  
#if defined(SGI_GL) && defined(GL_NASA)
  void start_radiosity( ap )
  Actuator *ap ;
#else
  void start_radiosity( val )
  int val ;
#endif
{
    static int state = 0 ;
    void radiosity() ;
    int init_ray_tasks() ;
    void init_radavg_tasks() ;
    int i;
    unsigned total_rad_time, max_rad_time, min_rad_time;
    unsigned total_refine_time, max_refine_time, min_refine_time;
    unsigned total_wait_time, max_wait_time, min_wait_time;
    unsigned total_vertex_time, max_vertex_time, min_vertex_time;
    
#if defined(SGI_GL) && defined(GL_NASA)
    int val ;
    
    val = g_get_choice_val( ap, &choices[0] ) ;
#endif
    
    if( val == CHOICE_RAD_RUN )
        {
            if( state == -1 )
                {
                    printf( "Please reset first\007\n" ) ;
                    return ;
                }
            
            /* Time stamp */
            {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(time_rad_start ) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
} ;
            
            
            global->index = 0;
            
            /* Create slave processes */
            for (i = 0 ; i < n_processors ; i++ )
                {
                    taskqueue_id[i] = assign_taskq(0) ;

                }
            
            {
	long	i, Error;

	for (i = 0; i < (n_processors) - 1; i++) {
		Error = pthread_create(&PThreadTable[i], NULL, (void * (*)(void *))(radiosity), NULL);
		if (Error != 0) {
			printf("Error in pthread_create().\n");
			exit(-1);
		}
	}

	radiosity();
};
            /* And start processing */
            //taskqueue_id[n_processors] = assign_taskq(0) ;
            //radiosity() ;
            
            {
	unsigned long	i, Error;
	for (i = 0; i < (n_processors) - 1; i++) {
		Error = pthread_join(PThreadTable[i], NULL);
		if (Error != 0) {
			printf("Error in pthread_join().\n");
			exit(-1);
		}
	}
};
            /* Time stamp */
            {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(time_rad_end ) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
            
            /* Print out running time */
            /* Print out running time */
            printf("TIMING STATISTICS MEASURED BY MAIN PROCESS:\n");
            
            print_running_time(0);
            
            if (dostats) {
                printf("\n\n\nPER-PROCESS STATISTICS:\n");
                
                printf("%8s%20s%20s%12s%12s\n","Proc","Total","Refine","Wait","Smooth");
                printf("%8s%20s%20s%12s%12s\n\n","","Time","Time","Time","Time")
                    ;
                for (i = 0; i < n_processors; i++)
                    printf("%8d%20lu%20lu%12lu%12lu\n",i,timing[i]->rad_time, timing[i]->refine_time, timing[i]->wait_time, timing[i]->vertex_time);
                
                total_rad_time = timing[0]->rad_time;
                max_rad_time = timing[0]->rad_time;
                min_rad_time = timing[0]->rad_time;
                
                total_refine_time = timing[0]->refine_time;
                max_refine_time = timing[0]->refine_time;
                min_refine_time = timing[0]->refine_time;
                
                total_wait_time = timing[0]->wait_time;
                max_wait_time = timing[0]->wait_time;
                min_wait_time = timing[0]->wait_time;
                
                total_vertex_time = timing[0]->vertex_time;
                max_vertex_time = timing[0]->vertex_time;
                min_vertex_time = timing[0]->vertex_time;
                
                for (i = 1; i < n_processors; i++) {
                    total_rad_time += timing[i]->rad_time;
                    if (timing[i]->rad_time > max_rad_time)
                        max_rad_time = timing[i]->rad_time;
                    if (timing[i]->rad_time < min_rad_time)
                        min_rad_time = timing[i]->rad_time;
                    
                    total_refine_time += timing[i]->refine_time;
                    if (timing[i]->refine_time > max_refine_time)
                        max_refine_time = timing[i]->refine_time;
                    if (timing[i]->refine_time < min_refine_time)
                        min_refine_time = timing[i]->refine_time;
                    
                    total_wait_time += timing[i]->wait_time;
                    if (timing[i]->wait_time > max_wait_time)
                        max_wait_time = timing[i]->wait_time;
                    if (timing[i]->wait_time < min_wait_time)
                        min_wait_time = timing[i]->wait_time;
                    
                    total_vertex_time += timing[i]->vertex_time;
                    if (timing[i]->vertex_time > max_vertex_time)
                        max_vertex_time = timing[i]->vertex_time;
                    if (timing[i]->vertex_time < min_vertex_time)
                        min_vertex_time = timing[i]->vertex_time;
                }
                
                
                printf("\n\n%8s%20lu%20lu%12lu%12lu\n","Max", max_rad_time, max_refine_time, max_wait_time, max_vertex_time);
                printf("\n%8s%20lu%20lu%12lu%12lu\n","Min", min_rad_time, min_refine_time, min_wait_time, min_vertex_time);
                printf("\n%8s%20lu%20lu%12lu%12lu\n","Avg", (int) (((double) total_rad_time) / ((double) (1.0 * n_processors))), (int) (((double) total_refine_time) / ((double) (1.0 * n_processors))), (int) (((double) total_wait_time) / ((double) (1.0 * n_processors))), (int) (((double) total_vertex_time) / ((double) (1.0 * n_processors))));
                printf("\n\n");
                
            }
            
            /*      print_fork_time(0) ; */
            
            print_statistics( stdout, 0 ) ;
            
            /* Display image */
            display_scene( disp_fill_mode, disp_patch_switch,
                          disp_mesh_switch, disp_interaction_switch, 0) ;
            
            state = -1 ;
        }
    
    else if( val == CHOICE_RAD_STEP )
        {
            if( state == -1 )
                {
                    printf( "Please reset first\007\n" ) ;
                    return ;
                }
            
            /* Step execution */
            switch( state )
                {
                case 0:
                    /* Step execute as a single process */
                    
                    global->index = 1;
                    /* Create slave processes */
                    for ( i = 0 ; i < n_processors ; i++ )
                        {
                            taskqueue_id[i] = assign_taskq(0) ;

                        }
                    
                    {
	long	i, Error;

	for (i = 0; i < (n_processors) - 1; i++) {
		Error = pthread_create(&PThreadTable[i], NULL, (void * (*)(void *))(radiosity), NULL);
		if (Error != 0) {
			printf("Error in pthread_create().\n");
			exit(-1);
		}
	}

	radiosity();
};
                    //taskqueue_id[n_processors] = assign_taskq(0) ;
                    
                    /* Decompose model objects into patches and build
                       the BSP tree */
                    /* Create the first tasks (MASTER only) */
                    init_modeling_tasks(0) ;
                    process_tasks(0) ;
                    state ++ ;
                    break ;
                    
                case 1:
                    if( init_ray_tasks(0) )
                        {
                            {
	unsigned long	Error, Cycle;
	int		Cancel, Temp;

	Error = pthread_mutex_lock(&(global->barrier).mutex);
	if (Error != 0) {
		printf("Error while trying to get lock in barrier.\n");
		exit(-1);
	}

	Cycle = (global->barrier).cycle;
	if (++(global->barrier).counter != (n_processors)) {
		pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &Cancel);
		while (Cycle == (global->barrier).cycle) {
			Error = pthread_cond_wait(&(global->barrier).cv, &(global->barrier).mutex);
			if (Error != 0) {
				break;
			}
		}
		pthread_setcancelstate(Cancel, &Temp);
	} else {
		(global->barrier).cycle = !(global->barrier).cycle;
		(global->barrier).counter = 0;
		Error = pthread_cond_broadcast(&(global->barrier).cv);
	}
	pthread_mutex_unlock(&(global->barrier).mutex);
};
                            process_tasks(0) ;
                        }
                    else
                        state++ ;
                    break ;
                default:
                    {
	unsigned long	Error, Cycle;
	int		Cancel, Temp;

	Error = pthread_mutex_lock(&(global->barrier).mutex);
	if (Error != 0) {
		printf("Error while trying to get lock in barrier.\n");
		exit(-1);
	}

	Cycle = (global->barrier).cycle;
	if (++(global->barrier).counter != (n_processors)) {
		pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &Cancel);
		while (Cycle == (global->barrier).cycle) {
			Error = pthread_cond_wait(&(global->barrier).cv, &(global->barrier).mutex);
			if (Error != 0) {
				break;
			}
		}
		pthread_setcancelstate(Cancel, &Temp);
	} else {
		(global->barrier).cycle = !(global->barrier).cycle;
		(global->barrier).counter = 0;
		Error = pthread_cond_broadcast(&(global->barrier).cv);
	}
	pthread_mutex_unlock(&(global->barrier).mutex);
};
                    init_radavg_tasks( RAD_AVERAGING_MODE, 0 ) ;
                    process_tasks(0) ;
                    init_radavg_tasks( RAD_NORMALIZING_MODE, 0 ) ;
                    process_tasks(0) ;
                    
                    {
	unsigned long	i, Error;
	for (i = 0; i < (n_processors-1) - 1; i++) {
		Error = pthread_join(PThreadTable[i], NULL);
		if (Error != 0) {
			printf("Error in pthread_join().\n");
			exit(-1);
		}
	}
}
                        state = -1 ;
                }
            
            /* Display image */
            display_scene( disp_fill_mode, disp_patch_switch,
                          disp_mesh_switch, disp_interaction_switch, 0) ;
        }
    
    else if( val == CHOICE_RAD_RESET )
        {
            /* Initialize global variables again */
            init_global(0) ;
            init_visibility_module(0) ;
            g_clear() ;
            state = 0 ;
        }
}


/***************************************
 *
 *    PANEL call back routine
 *
 *    change_display()   (MASTER only)
 *
 ****************************************/


#if defined(SGI_GL) && defined(GL_NASA)
void change_display( ap )
  
  Actuator *ap ;
#else
  void change_display( val )
  
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val ;
    
    val = g_get_choice_val( ap, &choices[1] ) ;
#endif
    
    /* Display image */
    switch( val )
        {
        case CHOICE_DISP_RADIOSITY:
            disp_fill_switch = (! disp_fill_switch) ;
            break ;
        case CHOICE_DISP_SHADED:
            disp_shade_switch = (! disp_shade_switch) ;
            break ;
        case CHOICE_DISP_PATCH:
            disp_patch_switch = (! disp_patch_switch) ;
            break ;
        case CHOICE_DISP_MESH:
            disp_mesh_switch = (! disp_mesh_switch) ;
            break ;
        case CHOICE_DISP_INTERACTION:
            disp_interaction_switch = (! disp_interaction_switch) ;
            break ;
        default:
            return ;
        }
    
    if( disp_fill_switch == 0 )
        disp_fill_mode = 0 ;
    else
        {
            if( disp_shade_switch == 0 )
                disp_fill_mode = 1 ;
            else
                disp_fill_mode = 2 ;
        }
    
    /* Display image */
    display_scene( disp_fill_mode, disp_patch_switch,
                  disp_mesh_switch, disp_interaction_switch, 0 ) ;
}


/*****************************************************
 *
 *    PANEL call back routine
 *
 *    change_view_y()            (MASTER only)
 *    change_BFepsilon()
 *    change_area_epsilon()
 *
 ******************************************************/

static void change_view()
{
    /* Change the view */
    setup_view( (float)disp_crnt_view_x, (float)disp_crnt_view_y,
               DFLT_VIEW_DIST, disp_crnt_zoom, 0 ) ;
    
    /* And redraw */
    display_scene( disp_fill_mode, disp_patch_switch,
                  disp_mesh_switch, disp_interaction_switch, 0 ) ;
}


#if defined(SGI_GL) && defined(GL_NASA)
void change_view_x( ap )
  Actuator *ap ;
#else
  void change_view_x( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_slide_val( ap ) ;
#endif
    
    /* Save current rot-X value */
    disp_crnt_view_x = val ;
    change_view() ;
}


#if defined(SGI_GL) && defined(GL_NASA)
void change_view_y( ap )
  Actuator *ap ;
#else
  void change_view_y( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_slide_val( ap ) ;
#endif
    
    /* Save current rot-Y value */
    disp_crnt_view_y = val ;
    change_view() ;
}


#if defined(SGI_GL) && defined(GL_NASA)
void change_view_zoom( ap )
  Actuator *ap ;
#else
  void change_view_zoom( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_slide_val( ap ) ;
#endif
    
    /* Save current zoom value */
    disp_crnt_zoom = (float)val / 10.0 ;
    change_view() ;
}


#if defined(SGI_GL) && defined(GL_NASA)
void change_BFepsilon( ap )
  Actuator *ap ;
#else
  void change_BFepsilon( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_slide_val( ap ) ;
#endif
    BFepsilon = (float)val / 1000.0 ;
}



#if defined(SGI_GL) && defined(GL_NASA)
void change_area_epsilon( ap )
  Actuator *ap ;
#else
  void change_area_epsilon( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_slide_val( ap ) ;
#endif
    Area_epsilon = (float)val ;
}


/***************************************
 *
 *    PANEL call back routine
 *
 *    select_model()   (MASTER only)
 *
 ****************************************/

#if defined(SGI_GL) && defined(GL_NASA)
void select_model( ap )
  Actuator *ap ;
#else
  void select_model( val )
  int val ;
#endif
{
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_choice_val( ap, &choices[2] ) ;
#endif
    switch( val )
        {
        case MODEL_TEST_DATA:
            model_selector = MODEL_TEST_DATA ;
            break ;
        case MODEL_ROOM_DATA:
            model_selector = MODEL_ROOM_DATA ;
            break ;
        case MODEL_LARGEROOM_DATA:
            model_selector = MODEL_LARGEROOM_DATA ;
            break ;
        }
}



/***************************************
 *
 *    PANEL call back routine
 *
 *    utility_tools()   (MASTER only)
 *
 ****************************************/


#if defined(SGI_GL) && defined(GL_NASA)
void utility_tools( ap )
  Actuator *ap ;
#else
  void utility_tools( val )
  int val ;
#endif
{
    FILE *fd ;
#if defined(SGI_GL) && defined(GL_NASA)
    int val = g_get_choice_val( ap, &choices[3] ) ;
#endif
    
    switch( val )
        {
        case CHOICE_UTIL_PS:
            /* Open PS file */
            ps_open( "radiosity.ps" ) ;
            
            /* Change the view */
            ps_setup_view( DFLT_VIEW_ROT_X, (float)disp_crnt_view_y,
                          DFLT_VIEW_DIST, DFLT_VIEW_ZOOM, 0 ) ;
            
            /* And redraw */
            ps_display_scene( disp_fill_mode, disp_patch_switch,
                             disp_mesh_switch, disp_interaction_switch, 0 ) ;
            
            /* Close */
            ps_close() ;
            break ;
        case CHOICE_UTIL_STAT_CRT:
            print_statistics( stdout, 0 ) ;
            break ;
        case CHOICE_UTIL_STAT_FILE:
            if( (fd = fopen( "radiosity_stat", "w" )) == 0 )
                {
                    perror( "radiosity_stat" ) ;
                    break ;
                }
            print_statistics( fd, 0 ) ;
            fclose( fd ) ;
            break ;
        case CHOICE_UTIL_CLEAR_RAD:
            clear_radiosity(0) ;
        }
}


/***************************************
 *
 *    Exposure call back
 *
 ****************************************/

static void expose_callback()
{
    /* Display image */
    display_scene( disp_fill_mode, disp_patch_switch,
                  disp_mesh_switch, disp_interaction_switch, 0 ) ;
}


/***************************************
 *
 *    radiosity()  Radiosity task main
 *
 ****************************************/


void radiosity()
{
    int init_ray_tasks() ;
    unsigned process_id;
    void init_radavg_tasks() ;
    unsigned long int rad_start, refine_done, vertex_start, vertex_done;
    
    {pthread_mutex_lock(&(global->index_lock));};
    process_id = global->index++;
    {pthread_mutex_unlock(&(global->index_lock));};
   
    printf("Radiosity 1 %d\n", process_id);
    fflush(stdout); 
    printf("Radiosity 2\n");
    fflush(stdout);
    process_id = process_id % n_processors;
    
    if ((process_id == 0) || (dostats))
        {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(rad_start) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
    
    /* POSSIBLE ENHANCEMENT:  Here is where one might pin processes to
       processors to avoid migration */

    /* POSSIBLE ENHANCEMENT:  Here is where one might reset the
       statistics that one is measuring about the parallel execution */

    /* Decompose model objects into patches and build the BSP tree */
    /* Create the initial tasks */
    printf("Radiosity 3\n");
    fflush(stdout);
    init_modeling_tasks(process_id) ;
    printf("Radiosity 3.5 %d\n", process_id);
    fflush(stdout);
    process_tasks(process_id) ;
    printf("Radiosity 4\n");
    fflush(stdout);
    
    /* Gather rays & do BF refinement */
    while( init_ray_tasks(process_id) )
        {
            /* Wait till tasks are put in the queue */
            {
	unsigned long	Error, Cycle;
	int		Cancel, Temp;

	Error = pthread_mutex_lock(&(global->barrier).mutex);
	if (Error != 0) {
		printf("Error while trying to get lock in barrier.\n");
		exit(-1);
	}

	Cycle = (global->barrier).cycle;
	if (++(global->barrier).counter != (n_processors)) {
		pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &Cancel);
		while (Cycle == (global->barrier).cycle) {
			Error = pthread_cond_wait(&(global->barrier).cv, &(global->barrier).mutex);
			if (Error != 0) {
				break;
			}
		}
		pthread_setcancelstate(Cancel, &Temp);
	} else {
		(global->barrier).cycle = !(global->barrier).cycle;
		(global->barrier).counter = 0;
		Error = pthread_cond_broadcast(&(global->barrier).cv);
	}
	pthread_mutex_unlock(&(global->barrier).mutex);
};
            /* Then perform ray-gathering and BF-refinement till the
               solution converges */
            process_tasks(process_id) ;
        }
    
    printf("Radiosity 5\n");
    if ((process_id == 0) || (dostats))
        {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(refine_done) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
    
    {
	unsigned long	Error, Cycle;
	int		Cancel, Temp;

	Error = pthread_mutex_lock(&(global->barrier).mutex);
	if (Error != 0) {
		printf("Error while trying to get lock in barrier.\n");
		exit(-1);
	}

	Cycle = (global->barrier).cycle;
	if (++(global->barrier).counter != (n_processors)) {
		pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &Cancel);
		while (Cycle == (global->barrier).cycle) {
			Error = pthread_cond_wait(&(global->barrier).cv, &(global->barrier).mutex);
			if (Error != 0) {
				break;
			}
		}
		pthread_setcancelstate(Cancel, &Temp);
	} else {
		(global->barrier).cycle = !(global->barrier).cycle;
		(global->barrier).counter = 0;
		Error = pthread_cond_broadcast(&(global->barrier).cv);
	}
	pthread_mutex_unlock(&(global->barrier).mutex);
};
    
    printf("Radiosity 6\n");
    if ((process_id == 0) || (dostats))
        {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(vertex_start) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
    
    /* Compute area-weighted radiosity value at each vertex */
    init_radavg_tasks( RAD_AVERAGING_MODE, process_id ) ;
    process_tasks(process_id) ;
    
    printf("Radiosity 7\n");
    /* Then normalize the radiosity at vertices */
    init_radavg_tasks( RAD_NORMALIZING_MODE, process_id ) ;
    process_tasks(process_id) ;
    
    if ((process_id == 0) || (dostats))
        {
	struct timeval	FullTime;

	gettimeofday(&FullTime, NULL);
	(vertex_done) = (unsigned long)(FullTime.tv_usec + FullTime.tv_sec * 1000000);
};
    
    if ((process_id == 0) || (dostats)) {
        timing[process_id]->rad_start = rad_start;
        timing[process_id]->rad_time = vertex_done - rad_start;
        timing[process_id]->refine_time = refine_done - rad_start;
        timing[process_id]->vertex_time = vertex_done - vertex_start;
        timing[process_id]->wait_time = vertex_start - refine_done;
    }
    
}



/***************************************************************************
 *
 *    init_ray_tasks()
 *
 *    Create initial tasks to perform ray gathering.
 *
 ****************************************************************************/


#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_STATIC
static void _init_ray_tasks_static() ;
#define _INIT_RAY_TASK  _init_ray_tasks_static
#endif
  
  
  static void _init_ray_tasks_cost2() ;
  static int avg_cost_of_q ;
  static int avg_cost_of_patch ;
  static int cost_of_this_q ;
  static int crnt_qid ;
  static int queue_cost[MAX_TASKQUEUES] ;
  
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
#define _INIT_RAY_TASK  _init_ray_tasks_cost2
#endif
  
  
  
  
  int init_ray_tasks(process_id)
  unsigned process_id;
{
    int conv ;
    
    /* If this is not the first process to initialize, then return */
    {pthread_mutex_lock(&(global->avg_radiosity_lock));};
    if( ! check_task_counter(process_id) )
        {
            conv = global->converged ;
            {pthread_mutex_unlock(&(global->avg_radiosity_lock));};
            return( conv == 0 ) ;
        }
    
    /* Check radiosity convergence */
    conv = radiosity_converged(process_id) ;
    global->converged = conv ;
    
    /* Reset total energy variable */
    global->prev_total_energy = global->total_energy ;
    global->total_energy.r = 0.0 ;
    global->total_energy.g = 0.0 ;
    global->total_energy.b = 0.0 ;
    global->total_patch_area = 0.0 ;
    
    /* Increment iteration counter */
    global->iteration_count++ ;
    {pthread_mutex_unlock(&(global->avg_radiosity_lock));};
    
    /* If radiosity converged, then return 0 */
    if( conv )
        return( 0 ) ;
    
    
#if PATCH_ASSIGNMENT == PATCH_ASSIGNMENT_COSTBASED
    /* Compute average cost per queue. Also reset the cost variable.
       The 'cost_sum' is not locked since no one is processing rays
       at this moment */
    
    for( crnt_qid = 0 ; crnt_qid < n_taskqueues ; crnt_qid++ )
        queue_cost[ crnt_qid ] = 0 ;
    
    avg_cost_of_q = global->cost_estimate_sum / n_taskqueues ;
    avg_cost_of_patch = global->cost_estimate_sum / global->n_total_patches ;
    cost_of_this_q = 0 ;
    crnt_qid = 0 ;
    
    global->cost_sum = 0 ;
    global->cost_estimate_sum = 0 ;
    
    /* layered selection of tasks */
    foreach_patch_in_bsp( _INIT_RAY_TASK, 2, process_id ) ;
    foreach_patch_in_bsp( _INIT_RAY_TASK, 1, process_id ) ;
#endif
    
    /* Create BF refinement tasks */
    foreach_patch_in_bsp( _INIT_RAY_TASK, 0, process_id ) ;
    
    return( 1 ) ;
}


static void _init_ray_tasks_static( p, dummy, process_id )
  Patch *p ;
  int dummy ;
  unsigned process_id;
{
    /* Clear incoming energy variable */
    p->el_root->rad_in.r = 0.0 ;
    p->el_root->rad_in.g = 0.0 ;
    p->el_root->rad_in.b = 0.0 ;
    
    enqueue_ray_task( (p->seq_no >> 2) % n_taskqueues, p->el_root,
                     TASK_APPEND, process_id ) ;
}


static void _init_ray_tasks_cost2( p, layer, process_id )
  Patch *p ;
  int layer ;
  unsigned process_id;
{
    Patch_Cost *pc ;
    int c_est ;
    int qid ;
    int min_cost_q, min_cost ;
    
    
    pc = &global->patch_cost[ p->seq_no ] ;
    c_est = pc->cost_estimate ;
    
    if( c_est < 0 )
        /* Already processed */
        return ;
    
    if( c_est < avg_cost_of_patch * layer )
        return ;
    
    /* Find the first available queue */
    min_cost_q = 0 ;
    min_cost = queue_cost[ 0 ] ;
    for( qid = 0 ; qid < n_taskqueues ; qid++ )
        {
            if( (c_est + queue_cost[ qid ]) <= avg_cost_of_q )
                break ;
            
            if( min_cost > queue_cost[ qid ] )
                {
                    min_cost_q = qid ;
                    min_cost = queue_cost[ qid ] ;
                }
        }
    
    if( qid >= n_taskqueues )
        {
            /* All queues are nearly full. Put to min-cost queue */
            qid = min_cost_q ;
        }
    
    /* Update queue cost */
    queue_cost[ qid ] += c_est ;
    
    
    /* Clear incoming energy variable */
    p->el_root->rad_in.r = 0.0 ;
    p->el_root->rad_in.g = 0.0 ;
    p->el_root->rad_in.b = 0.0 ;
    
    /* Clear cost value */
    pc->cost_estimate = -1 ;
    pc->n_bsp_node    = 0 ;
    
    /* Enqueue */
    enqueue_ray_task( qid, p->el_root, TASK_APPEND, process_id ) ;
    
}




/***************************************************************************
 *
 *    init_radavg_tasks()
 *
 *    Create initial tasks to perform radiosity averaging.
 *
 ****************************************************************************/

static void _init_radavg_tasks() ;
  
  void init_radavg_tasks( mode, process_id )
  
  int mode ;
  unsigned process_id;
{
    
    /* If this is not the first process to initialize, then return */
    if( ! check_task_counter(process_id ) )
        return ;
    
    /* Create RadAvg tasks */
    foreach_patch_in_bsp( _init_radavg_tasks, mode, process_id ) ;
}


static void _init_radavg_tasks( p, mode, process_id )
  Patch *p ;
  int mode ;
  unsigned process_id;
{
    enqueue_radavg_task( p->seq_no % n_taskqueues, p->el_root, mode, process_id  ) ;
}



/***************************************
 *
 *    init_global()
 *
 ****************************************/


void init_global(process_id)
  unsigned process_id;
{
    /* Clear BSP root pointer */
    global->index = 1;  /* ****** */
    global->bsp_root = 0 ;
    {pthread_mutex_init(&(global->index_lock), NULL);};
    {pthread_mutex_init(&(global->bsp_tree_lock), NULL);};
    
    /* Initialize radiosity statistics variables */
    {pthread_mutex_init(&(global->avg_radiosity_lock), NULL);};
    global->converged = 0 ;
    global->prev_total_energy.r = 0.0 ;   
    global->prev_total_energy.g = 0.0 ;   
    global->prev_total_energy.b = 0.0 ;   
    global->total_energy.r = 1.0 ;   
    global->total_energy.g = 1.0 ;   
    global->total_energy.b = 1.0 ;   
    global->total_patch_area = 1.0 ;
    global->iteration_count = -1 ;     /* init_ray_task() increments to 0 */
    
    /* Initialize the cost sum */
    {pthread_mutex_init(&(global->cost_sum_lock), NULL);};
    global->cost_sum = 0 ;
    global->cost_estimate_sum = 0 ;
    
    /* Initialize the barrier */
    {
	unsigned long	Error;

	Error = pthread_mutex_init(&(global->barrier).mutex, NULL);
	if (Error != 0) {
		printf("Error while initializing barrier.\n");
		exit(-1);
	}

	Error = pthread_cond_init(&(global->barrier).cv, NULL);
	if (Error != 0) {
		printf("Error while initializing barrier.\n");
		pthread_mutex_destroy(&(global->barrier).mutex);
		exit(-1);
	}

	(global->barrier).counter = 0;
	(global->barrier).cycle = 0;
};
    {pthread_mutex_init(&(global->pbar_lock), NULL);};
    global->pbar_count = 0 ;
    
    /* Initialize task counter */
    global->task_counter = 0 ;
    {pthread_mutex_init(&(global->task_counter_lock), NULL);};
    
    /* Initialize task queue */
    init_taskq(process_id) ;
    
    /* Initialize Patch, Element, Interaction free lists */
    init_patchlist(process_id) ;
    init_elemlist(process_id) ;
    init_interactionlist(process_id) ;
    init_elemvertex(process_id) ;
    init_edge(process_id) ;
    
    /* Initialize statistical info */
    init_stat_info(process_id) ;
    
}


/*************************************************************
 *
 * parse_args()   Parse arguments
 *
 **************************************************************/

static void parse_args( ac, av )
  
  int ac ;
  char *av[] ;
{
    int cnt ;
    
    /* Parse arguments */
    for( cnt = 1 ; cnt < ac ; cnt++ ) 
        {
            if( strcmp( av[cnt], "-p" ) == 0 ) {
                sscanf( av[++cnt], "%d", &n_processors ) ;
                n_taskqueues = n_processors;
            }	
            else if( strcmp( av[cnt], "-tq" ) == 0 ) 
                sscanf( av[++cnt], "%d", &n_tasks_per_queue ) ;
            else if( strcmp( av[cnt], "-ae" ) == 0 )
                sscanf( av[++cnt], "%f", &Area_epsilon ) ;
            else if( strcmp( av[cnt], "-pr" ) == 0 ) 
                sscanf( av[++cnt], "%d", &N_inter_parallel_bf_refine ) ;
            else if( strcmp( av[cnt], "-pv" ) == 0 ) 
                sscanf( av[++cnt], "%d", &N_visibility_per_task ) ;
            else if( strcmp( av[cnt], "-bf" ) == 0 )
                sscanf( av[++cnt], "%f", &BFepsilon ) ;
            else if( strcmp( av[cnt], "-en" ) == 0 )
                sscanf( av[++cnt], "%f", &Energy_epsilon ) ;
            
            else if( strcmp( av[cnt], "-batch" ) == 0 )
                batch_mode = 1 ;
            else if( strcmp( av[cnt], "-verbose" ) == 0 )
                verbose_mode = 1 ;
            else if( strcmp( av[cnt], "-s" ) == 0 )
                dostats = 1 ;
            else if( strcmp( av[cnt], "-room" ) == 0 )
                model_selector = MODEL_ROOM_DATA ;
            else if( strcmp( av[cnt], "-largeroom" ) == 0 )
                model_selector = MODEL_LARGEROOM_DATA ;
            else if(( strcmp( av[cnt], "-help" ) == 0 ) || ( strcmp( av[cnt], "-h" ) == 0 ) || ( strcmp( av[cnt], "-H" ) == 0 ))	    {
                print_usage() ;
                exit(0) ;
            }
        }
    
    
    /* Then check the arguments */
    if( (n_processors < 1) || (MAX_PROCESSORS < n_processors) )
        {
            fprintf( stderr, "Bad number of processors: %d\n",
                    n_processors ) ;
            exit(1) ;
        }
    if( (n_taskqueues < 1) || (MAX_TASKQUEUES < n_taskqueues) )
        {
            fprintf( stderr, "Bad number of task queues: %d\n",
                    n_taskqueues ) ;
            exit(1) ;
        }
    /* Check epsilon values */
    if( Area_epsilon < 0.0 )
        {
            fprintf( stderr, "Area epsilon must be positive\n" ) ;
            exit(1) ;
        }
    if( BFepsilon < 0.0 )
        {
            fprintf( stderr, "BFepsilon must be within [0,1]\n" ) ;
            exit(1) ;
        }
}



/*************************************************************
 *
 *   print_usage()
 *
 **************************************************************/

void print_usage()
{
    fprintf( stderr, "Usage:  RADIOSITY  [options..]\n\n" ) ;
    fprintf( stderr, "\tNote: Must have a space between option label and numeric value, if any\n\n");
    fprintf( stderr, "   -p    (d)  # of processes\n" ) ;
    fprintf( stderr, "   -tq   (d)  # of tasks per queue: default (200) in code for SPLASH\n" ) ;
    fprintf( stderr, "   -ae   (f)  Area epsilon: default (2000.0) in code for SPLASH\n" ) ;
    fprintf( stderr, "   -pr   (d)  # of inter for parallel refinement: default (5) in code for SPLASH\n") ;
    fprintf( stderr, "   -pv   (d)  # of visibility comp in a task: default (4) in code for SPLASH\n") ;
    fprintf( stderr, "   -bf   (f)  BFepsilon (BF refinement): default (0.015) in code for SPLASH\n" ) ;
    fprintf( stderr, "   -en   (f)  Energy epsilon (convergence): default (0.005) in code for SPLASH\n" ) ;
    fprintf( stderr, "   -room      Use room model (default=test)\n" ) ;
    fprintf( stderr, "   -largeroom Use large room model\n" ) ;
    fprintf( stderr, "   -batch     Batch mode (use for SPLASH)\n" ) ;
    fprintf( stderr, "   -verbose   Verbose mode (don't use for SPLASH)\n" ) ;
    fprintf( stderr, "   -s   Measure per-process timing (don't use for SPLASH)\n" ) ;
}

